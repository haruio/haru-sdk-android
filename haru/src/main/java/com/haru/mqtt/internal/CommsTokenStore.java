/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package com.haru.mqtt.internal;

import com.haru.mqtt.MqttDeliveryToken;
import com.haru.mqtt.MqttException;
import com.haru.mqtt.MqttToken;
import com.haru.mqtt.internal.wire.MqttPublish;
import com.haru.mqtt.internal.wire.MqttWireMessage;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * Provides a "token" based system for storing and tracking actions across 
 * multiple threads. 
 * When a message is sent, a token is associated with the message
 * and saved using the {@link #saveToken(MqttToken, MqttWireMessage)} method. Anyone interested
 * in tacking the state can call one of the wait methods on the token or using 
 * the asynchronous listener callback method on the operation. 
 * The {@link CommsReceiver} class, on another thread, reads responses back from 
 * the network. It uses the response to find the relevant token, which it can then 
 * notify. 
 * 
 * Note:
 *   Ping, connect and disconnect do not have a unique message id as
 *   only one outstanding request of each type is allowed to be outstanding
 */
public class CommsTokenStore {

	// Maps message-specific data (usually message IDs) to tokens
	private Hashtable tokens;
	private String logContext;
	private MqttException closedResponse = null;

	public CommsTokenStore(String logContext) {
		this.tokens = new Hashtable();
		this.logContext = logContext;

	}

	/**
	 * Based on the message type that has just been received return the associated
	 * token from the token store or null if one does not exist.
	 * @param message whose token is to be returned 
	 * @return token for the requested message
	 */
	public MqttToken getToken(MqttWireMessage message) {
		String key = message.getKey(); 
		return (MqttToken)tokens.get(key);
	}

	public MqttToken getToken(String key) {
		return (MqttToken)tokens.get(key);
	}

	
	public MqttToken removeToken(MqttWireMessage message) {
		if (message != null) {
			return removeToken(message.getKey());
		}
		return null;
	}
	
	public MqttToken removeToken(String key) {

		if ( null != key ){
		    return (MqttToken) tokens.remove(key);
		}
		
		return null;
	}
		
	/**
	 * Restores a token after a client restart.  This method could be called
	 * for a SEND of CONFIRM, but either way, the original SEND is what's 
	 * needed to re-build the token.
	 */
	protected MqttDeliveryToken restoreToken(MqttPublish message) {
		MqttDeliveryToken token;
		synchronized(tokens) {
			String key = new Integer(message.getMessageId()).toString();
			if (this.tokens.containsKey(key)) {
				token = (MqttDeliveryToken)this.tokens.get(key);
			} else {
				token = new MqttDeliveryToken(logContext);
				token.internalTok.setKey(key);
				this.tokens.put(key, token);
			}
		}
		return token;
	}
	
	// For outbound messages store the token in the token store 
	// For pubrel use the existing publish token 
	protected void saveToken(MqttToken token, MqttWireMessage message) throws MqttException {

		synchronized(tokens) {
			if (closedResponse == null) {
				String key = message.getKey();

				saveToken(token,key);
			} else {
				throw closedResponse;
			}
		}
	}
	
	protected void saveToken(MqttToken token, String key) {

		synchronized(tokens) {
			token.internalTok.setKey(key);
			this.tokens.put(key, token);
		}
	}

	protected void quiesce(MqttException quiesceResponse) {

		synchronized(tokens) {

			closedResponse = quiesceResponse;
		}
	}
	
	public void open() {

		synchronized(tokens) {

			closedResponse = null;
		}
	}

	public MqttDeliveryToken[] getOutstandingDelTokens() {

		synchronized(tokens) {

			Vector list = new Vector();
			Enumeration enumeration = tokens.elements();
			MqttToken token;
			while(enumeration.hasMoreElements()) {
				token = (MqttToken)enumeration.nextElement();
				if (token != null 
					&& token instanceof MqttDeliveryToken 
					&& !token.internalTok.isNotified()) {
					
					list.addElement(token);
				}
			}
	
			MqttDeliveryToken[] result = new MqttDeliveryToken[list.size()];
			return (MqttDeliveryToken[]) list.toArray(result);
		}
	}
	
	public Vector getOutstandingTokens() {

		synchronized(tokens) {

			Vector list = new Vector();
			Enumeration enumeration = tokens.elements();
			MqttToken token;
			while(enumeration.hasMoreElements()) {
				token = (MqttToken)enumeration.nextElement();
				if (token != null) {
						list.addElement(token);
				}
			}
			return list;
		}
	}

	/**
	 * Empties the token store without notifying any of the tokens.
	 */
	public void clear() {
		synchronized(tokens) {
			tokens.clear();
		}
	}
	
	public int count() {
		synchronized(tokens) {
			return tokens.size();
		}
	}
	public String toString() {
		String lineSep = System.getProperty("line.separator","\n");
		StringBuffer toks = new StringBuffer();
		synchronized(tokens) {
			Enumeration enumeration = tokens.elements();
			MqttToken token;
			while(enumeration.hasMoreElements()) {
				token = (MqttToken)enumeration.nextElement();
					toks.append("{"+token.internalTok+"}"+lineSep);
			}
			return toks.toString();
		}
	}
}
