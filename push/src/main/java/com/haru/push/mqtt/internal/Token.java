/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
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
 *   Ian Craggs - MQTT 3.1.1 support
 */

package com.haru.push.mqtt.internal;

import com.haru.push.mqtt.IMqttActionListener;
import com.haru.push.mqtt.IMqttClient;
import com.haru.push.mqtt.MqttException;
import com.haru.push.mqtt.MqttMessage;
import com.haru.push.mqtt.internal.wire.MqttAck;
import com.haru.push.mqtt.internal.wire.MqttConnack;
import com.haru.push.mqtt.internal.wire.MqttSuback;
import com.haru.push.mqtt.internal.wire.MqttWireMessage;

public class Token {
	private volatile boolean completed = false;
	private boolean pendingComplete = false;
	private boolean sent = false;
	
	private Object responseLock = new Object();
	private Object sentLock = new Object();
	
	protected MqttMessage message = null;
	private MqttWireMessage response = null;
	private MqttException exception = null;
	private String[] topics = null;
	
	private String key;
	
	private IMqttClient client = null;
	private IMqttActionListener callback = null;
	
	private Object userContext = null;
	
	private int messageID = 0;
	private boolean notified = false;
	
	public Token(String logContext) {
	}
	
	public int getMessageID() {
		return messageID;
	}

	public void setMessageID(int messageID) {
		this.messageID = messageID;
	}
	
	public boolean checkResult() throws MqttException {
		if ( getException() != null)  {
			throw getException();
		}
		return true;
	}

	public MqttException getException() {
		return exception;
	}

	public boolean isComplete() {
		return completed;
	}

	protected boolean isCompletePending() {
		return pendingComplete;
	}

	protected boolean isInUse() {
		return (getClient() != null && !isComplete());
	}

	public void setActionCallback(IMqttActionListener listener) {
		this.callback  = listener;

	}
	public IMqttActionListener getActionCallback() {
		return callback;
	}

	public void waitForCompletion() throws MqttException {
		waitForCompletion(-1);
	}

	public void waitForCompletion(long timeout) throws MqttException {

		MqttWireMessage resp = waitForResponse(timeout);
		if (resp == null && !completed) {
			exception = new MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
			throw exception;
		}
		checkResult();
	}
	
	/**
	 * Waits for the message delivery to complete, but doesn't throw an exception
	 * in the case of a NACK.  It does still throw an exception if something else
	 * goes wrong (e.g. an IOException).  This is used for packets like CONNECT, 
	 * which have useful information in the ACK that needs to be accessed.
	 */
	protected MqttWireMessage waitForResponse() throws MqttException {
		return waitForResponse(-1);
	}
	
	protected MqttWireMessage waitForResponse(long timeout) throws MqttException {
		synchronized (responseLock) {

			while (!this.completed) {
				if (this.exception == null) {
					try {
	
						if (timeout <= 0) {
							responseLock.wait();
						} else {
							responseLock.wait(timeout);
						}
					} catch (InterruptedException e) {
						exception = new MqttException(e);
					}
				}
				if (!this.completed) {
					if (this.exception != null) {
						throw exception;
					}
					
					if (timeout > 0) {
						// time up and still not completed
						break;
					}
				}
			}
		}
		return this.response;
	}
	
	/**
	 * Mark the token as complete and ready for users to be notified.
	 * @param msg response message. Optional - there are no response messages for some flows
	 * @param ex if there was a problem store the exception in the token.
	 */
	protected void markComplete(MqttWireMessage msg, MqttException ex) {

		synchronized(responseLock) {
			// ACK means that everything was OK, so mark the message for garbage collection.
			if (msg instanceof MqttAck) {
				this.message = null;
			}
			this.pendingComplete = true;
			this.response = msg;
			this.exception = ex;
		}
	}
	/**
	 * Notifies this token that a response message (an ACK or NACK) has been
	 * received.
	 */
		protected void notifyComplete() {

			synchronized (responseLock) {
				// If pending complete is set then normally the token can be marked
				// as complete and users notified. An abnormal error may have 
				// caused the client to shutdown beween pending complete being set
				// and notifying the user.  In this case - the action must be failed.
				if (exception == null && pendingComplete) {
					completed = true;
					pendingComplete = false;
				} else {
					pendingComplete = false;
				}
				
				responseLock.notifyAll();
			}
			synchronized (sentLock) {
				sent=true;	
				sentLock.notifyAll();
			}
		}
	
//	/**
//	 * Notifies this token that an exception has occurred.  This is only
//	 * used for things like IOException, and not for MQTT NACKs.
//	 */
//	protected void notifyException() {
//		synchronized (responseLock) {
//			responseLock.notifyAll();
//		}
//		synchronized (sentLock) {
//			sentLock.notifyAll();
//		}
//	}

	public void waitUntilSent() throws MqttException {
		synchronized (sentLock) {
			synchronized (responseLock) {
				if (this.exception != null) {
					throw this.exception;
				}
			}
			while (!sent) {
				try {

					sentLock.wait();
				} catch (InterruptedException e) {
				}
			}
			
			while (!sent) {
				if (this.exception == null) {
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
				}
				throw this.exception;
			}
		}
	}
	
	/**
	 * Notifies this token that the associated message has been sent
	 * (i.e. written to the TCP/IP socket).
	 */
	protected void notifySent() {
		synchronized (responseLock) {
			this.response = null;
			this.completed = false;
		}
		synchronized (sentLock) {
			sent = true;
			sentLock.notifyAll();
		}
	}
	
	public IMqttClient getClient() {
		return client;
	}
	
	protected void setClient(IMqttClient client) {
		this.client = client;
	}

	public void reset() throws MqttException {
		if (isInUse() ) {
			// Token is already in use - cannot reset 
			throw new MqttException(MqttException.REASON_CODE_TOKEN_INUSE);
		}
		
		client = null;
		completed = false;
		response = null;
		sent = false;
		exception = null;
		userContext = null;
	}

	public MqttMessage getMessage() {
		return message;
	}
	
	public MqttWireMessage getWireMessage() {
		return response;
	}

	
	public void setMessage(MqttMessage msg) {
		this.message = msg;
	}
	
	public String[] getTopics() {
		return topics;
	}
	
	public void setTopics(String[] topics) {
		this.topics = topics;
	}
	
	public Object getUserContext() {
		return userContext;
	}

	public void setUserContext(Object userContext) {
		this.userContext = userContext;	
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public void setException(MqttException exception) {
		synchronized(responseLock) {
			this.exception = exception;
		}
	}

	public boolean isNotified() {
		return notified;
	}

	public void setNotified(boolean notified) {
		this.notified = notified;
	}

	public String toString() {
		StringBuffer tok = new StringBuffer();
		tok.append("key=").append(getKey());
		tok.append(" ,topics=");
		if (getTopics() != null) {
			for (int i=0; i<getTopics().length; i++) {
				tok.append(getTopics()[i]).append(", ");
			} 
		}
		tok.append(" ,usercontext=").append(getUserContext());
		tok.append(" ,isComplete=").append(isComplete());
		tok.append(" ,isNotified=").append(isNotified());
		tok.append(" ,exception=").append(getException());
		tok.append(" ,actioncallback=").append(getActionCallback());

		return tok.toString();
	}
	
	public int[] getGrantedQos() {
		int[] val = new int[0];
		if (response instanceof MqttSuback) {
			val = ((MqttSuback)response).getGrantedQos();
		}
		return val;
	}
	
	public boolean getSessionPresent() {
		boolean val = false;
		if (response instanceof MqttConnack) {
			val = ((MqttConnack)response).getSessionPresent();
		}
		return val;
	}
	
	public MqttWireMessage getResponse() {
		return response;
	}

}
