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

import com.haru.mqtt.IMqttAsyncClient;
import com.haru.mqtt.MqttCallback;
import com.haru.mqtt.MqttClientPersistence;
import com.haru.mqtt.MqttConnectOptions;
import com.haru.mqtt.MqttDeliveryToken;
import com.haru.mqtt.MqttException;
import com.haru.mqtt.MqttPersistenceException;
import com.haru.mqtt.MqttPingSender;
import com.haru.mqtt.MqttToken;
import com.haru.mqtt.MqttTopic;
import com.haru.mqtt.internal.wire.MqttConnack;
import com.haru.mqtt.internal.wire.MqttConnect;
import com.haru.mqtt.internal.wire.MqttDisconnect;
import com.haru.mqtt.internal.wire.MqttPublish;
import com.haru.mqtt.internal.wire.MqttWireMessage;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * Handles client communications with the server.  Sends and receives MQTT V3
 * messages.
 */
public class ClientComms {
	public static String 		VERSION = "3.2";
	public static String 		BUILD_LEVEL = "release";

	private static final byte CONNECTED	= 0;
	private static final byte CONNECTING	= 1;
	private static final byte DISCONNECTING	= 2;
	private static final byte DISCONNECTED	= 3;
	private static final byte CLOSED	= 4;

	private IMqttAsyncClient client;
	private int 					networkModuleIndex;
	private NetworkModule[]			networkModules;
	private CommsReceiver 			receiver;
	private CommsSender 			sender;
	private CommsCallback 			callback;
	private ClientState	 			clientState;
	private MqttConnectOptions conOptions;
	private MqttClientPersistence persistence;
	private MqttPingSender pingSender;
	private CommsTokenStore 		tokenStore;
	private boolean 				stoppingComms = false;

	private byte	conState = DISCONNECTED;
	private Object	conLock = new Object();  	// Used to synchronize connection state
	private boolean	closePending = false;

	/**
	 * Creates a new ClientComms object, using the specified module to handle
	 * the network calls.
	 */
	public ClientComms(IMqttAsyncClient client, MqttClientPersistence persistence, MqttPingSender pingSender) throws MqttException {
		this.conState = DISCONNECTED;
		this.client 	= client;
		this.persistence = persistence;
		this.pingSender = pingSender;
		this.pingSender.init(this);
		
		this.tokenStore = new CommsTokenStore(getClient().getClientId());
		this.callback 	= new CommsCallback(this);
		this.clientState = new ClientState(persistence, tokenStore, this.callback, this, pingSender);

		callback.setClientState(clientState);
	}

	CommsReceiver getReceiver() {
		return receiver;
	}

	/**
	 * Sends a message to the server. Does not check if connected this validation must be done
	 * by invoking routines.
	 * @param message
	 * @param token
	 * @throws MqttException
	 */
	void internalSend(MqttWireMessage message, MqttToken token) throws MqttException {

		if (token.getClient() == null ) {
			// Associate the client with the token - also marks it as in use.
			token.internalTok.setClient(getClient());
		} else {
			// Token is already in use - cannot reuse

			throw new MqttException(MqttException.REASON_CODE_TOKEN_INUSE);
		}

		try {
			// Persist if needed and send the message
			this.clientState.send(message, token);
		} catch(MqttException e) {
			if (message instanceof MqttPublish) {
				this.clientState.undo((MqttPublish)message);
			}
			throw e;
		}
	}

	/**
	 * Sends a message to the broker if in connected state, but only waits for the message to be
	 * stored, before returning.
	 */
	public void sendNoWait(MqttWireMessage message, MqttToken token) throws MqttException {
		if (isConnected() ||
				(!isConnected() && message instanceof MqttConnect) ||
				(isDisconnecting() && message instanceof MqttDisconnect)) {
			this.internalSend(message, token);
		} else {
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
		}
	}

	/**
	 * Close and tidy up.
	 *
	 * Call each main class and let it tidy up e.g. releasing the token
	 * store which normally survives a disconnect.
	 * @throws MqttException  if not disconnected
	 */
	public void close() throws MqttException {
		synchronized (conLock) {
			if (!isClosed()) {
				// Must be disconnected before close can take place
				if (!isDisconnected()) {

					if (isConnecting()) {
						throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
					} else if (isConnected()) {
						throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_CONNECTED);
					} else if (isDisconnecting()) {
						closePending = true;
						return;
					}
				}

				conState = CLOSED;

				// ShutdownConnection has already cleaned most things
				clientState.close();
				clientState = null;
				callback = null;
				persistence = null;
				sender = null;
				pingSender = null;
				receiver = null;
				networkModules = null;
				conOptions = null;
				tokenStore = null;
			}
		}
	}

	/**
	 * Sends a connect message and waits for an ACK or NACK.
	 * Connecting is a special case which will also start up the
	 * network connection, receive thread, and keep alive thread.
	 */
	public void connect(MqttConnectOptions options, MqttToken token) throws MqttException {
		synchronized (conLock) {
			if (isDisconnected() && !closePending) {
				conState = CONNECTING;

				this.conOptions = options;

				MqttConnect connect = new MqttConnect(client.getClientId(),
						options.getMqttVersion(),
						options.isCleanSession(),
						options.getKeepAliveInterval(),
						options.getUserName(),
						options.getPassword(),
						options.getWillMessage(),
						options.getWillDestination());

				this.clientState.setKeepAliveSecs(options.getKeepAliveInterval());
				this.clientState.setCleanSession(options.isCleanSession());

				tokenStore.open();
				ConnectBG conbg = new ConnectBG(this, token, connect);
				conbg.start();
			}
			else {
				if (isClosed() || closePending) {
					throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
				} else if (isConnecting()) {
					throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
				} else if (isDisconnecting()) {
					throw new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
				} else {
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_CONNECTED);
				}
			}
		}
	}

	public void connectComplete( MqttConnack cack, MqttException mex) throws MqttException {
		int rc = cack.getReturnCode();
		synchronized (conLock) {
			if (rc == 0) {
				// We've successfully connected
				conState = CONNECTED;
				return;
			}
		}

		throw mex;
	}

	/**
	 * Shuts down the connection to the server.
	 * This may have been invoked as a result of a user calling disconnect or
	 * an abnormal disconnection.  The method may be invoked multiple times
	 * in parallel as each thread when it receives an error uses this method
	 * to ensure that shutdown completes successfully.
	 */
	public void shutdownConnection(MqttToken token, MqttException reason) {
		boolean wasConnected;
		MqttToken endToken = null; 		//Token to notify after disconnect completes

		// This method could concurrently be invoked from many places only allow it
		// to run once.
		synchronized(conLock) {
			if (stoppingComms || closePending) {
				return;
			}
			stoppingComms = true;

			wasConnected = (isConnected() || isDisconnecting());
			conState = DISCONNECTING;
		}

		// Update the token with the reason for shutdown if it
		// is not already complete.
		if (token != null && !token.isComplete()) {
			token.internalTok.setException(reason);
		}

		// Stop the thread that is used to call the user back
		// when actions complete
		if (callback!= null) {callback.stop(); }

		// Stop the network module, send and receive now not possible
		try {
			if (networkModules != null) {
				NetworkModule networkModule = networkModules[networkModuleIndex];
				if (networkModule != null) {
					networkModule.stop();
				}
			}
		} catch (Exception ioe) {
			// Ignore as we are shutting down
		}

		// Stop the thread that handles inbound work from the network
		if (receiver != null) {receiver.stop();}

		// Stop any new tokens being saved by app and throwing an exception if they do
		tokenStore.quiesce(new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING));

		// Notify any outstanding tokens with the exception of
		// con or discon which may be returned and will be notified at
		// the end
		endToken = handleOldTokens(token, reason);

		try {
			// Clean session handling and tidy up
			clientState.disconnected(reason);
		}catch(Exception ex) {
			// Ignore as we are shutting down
		}

		if (sender != null) { sender.stop(); }
		
		if (pingSender != null){
			pingSender.stop();
		}

		try {
			if (persistence != null) {persistence.close();}
		}catch(Exception ex) {
			// Ignore as we are shutting down
		}
		// All disconnect logic has been completed allowing the
		// client to be marked as disconnected.
		synchronized(conLock) {
			conState = DISCONNECTED;
			stoppingComms = false;
		}

		// Internal disconnect processing has completed.  If there
		// is a disconnect token or a connect in error notify
		// it now. This is done at the end to allow a new connect
		// to be processed and now throw a currently disconnecting error.
		// any outstanding tokens and unblock any waiters
		if (endToken != null & callback != null) {
			callback.asyncOperationComplete(endToken);
		}

		if (wasConnected && callback != null) {
			// Let the user know client has disconnected either normally or abnormally
			callback.connectionLost(reason);
		}

		// While disconnecting, close may have been requested - try it now
		synchronized(conLock) {
			if (closePending) {
				try {
					close();
				} catch (Exception e) { // ignore any errors as closing
				}
			}
		}
	}

	// Tidy up. There may be tokens outstanding as the client was
	// not disconnected/quiseced cleanly! Work out what tokens still
	// need to be notified and waiters unblocked. Store the
	// disconnect or connect token to notify after disconnect is
	// complete.
	private MqttToken handleOldTokens(MqttToken token, MqttException reason) {

		MqttToken tokToNotifyLater = null;
		try {
			// First the token that was related to the disconnect / shutdown may
			// not be in the token table - temporarily add it if not
			if (token != null) {
				if (tokenStore.getToken(token.internalTok.getKey())==null) {
					tokenStore.saveToken(token, token.internalTok.getKey());
				}
			}

			Vector toksToNot = clientState.resolveOldTokens(reason);
			Enumeration toksToNotE = toksToNot.elements();
			while(toksToNotE.hasMoreElements()) {
				MqttToken tok = (MqttToken)toksToNotE.nextElement();

				if (tok.internalTok.getKey().equals(MqttDisconnect.KEY) ||
						tok.internalTok.getKey().equals(MqttConnect.KEY)) {
					// Its con or discon so remember and notify @ end of disc routine
					tokToNotifyLater = tok;
				} else {
					// notify waiters and callbacks of outstanding tokens
					// that a problem has occurred and disconnect is in
					// progress
					callback.asyncOperationComplete(tok);
				}
			}
		}catch(Exception ex) {
			// Ignore as we are shutting down
		}
		return tokToNotifyLater;
	}

	public void disconnect(MqttDisconnect disconnect, long quiesceTimeout, MqttToken token) throws MqttException {
		synchronized (conLock){
			if (isClosed()) {
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
			} else if (isDisconnected()) {
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED);
			} else if (isDisconnecting()) {
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
			} else if (Thread.currentThread() == callback.getThread()) {
				// Not allowed to call disconnect() from the callback, as it will deadlock.
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED);
			}

			conState = DISCONNECTING;
			DisconnectBG discbg = new DisconnectBG(disconnect,quiesceTimeout,token);
			discbg.start();
		}
	}
	
	/**
	 * Disconnect the connection and reset all the states.
	 */
	public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException {
		// Allow current inbound and outbound work to complete
		clientState.quiesce(quiesceTimeout);
		MqttToken token = new MqttToken(client.getClientId());
		try {
			// Send disconnect packet
			internalSend(new MqttDisconnect(), token);

			// Wait util the disconnect packet sent with timeout
			token.waitForCompletion(disconnectTimeout);
		}
		catch (Exception ex) {
			// ignore, probably means we failed to send the disconnect packet.
		}
		finally {
			token.internalTok.markComplete(null, null);
			shutdownConnection(token, null);
		}
	}

	public boolean isConnected() {
		synchronized (conLock) {
			return conState == CONNECTED;
		}
	}

	public boolean isConnecting() {
		synchronized (conLock) {
			return conState == CONNECTING;
		}
	}

	public boolean isDisconnected() {
		synchronized (conLock) {
			return conState == DISCONNECTED;
		}
	}

	public boolean isDisconnecting() {
		synchronized (conLock) {
			return conState == DISCONNECTING;
		}
	}

	public boolean isClosed() {
		synchronized (conLock) {
			return conState == CLOSED;
		}
	}


	public void setCallback(MqttCallback mqttCallback) {
		this.callback.setCallback(mqttCallback);
	}

	protected MqttTopic getTopic(String topic) {
		return new MqttTopic(topic, this);
	}
	public void setNetworkModuleIndex(int index) {
		this.networkModuleIndex = index;
	}
	public int getNetworkModuleIndex() {
		return networkModuleIndex;
	}
	public NetworkModule[] getNetworkModules() {
		return networkModules;
	}
	public void setNetworkModules(NetworkModule[] networkModules) {
		this.networkModules = networkModules;
	}
	public MqttDeliveryToken[] getPendingDeliveryTokens() {
		return tokenStore.getOutstandingDelTokens();
	}
	protected void deliveryComplete(MqttPublish msg) throws MqttPersistenceException {
		this.clientState.deliveryComplete(msg);
	}

	public IMqttAsyncClient getClient() {
		return client;
	}

	public long getKeepAlive() {
		return this.clientState.getKeepAlive();
	}

	public ClientState getClientState() {
		return clientState;
	}

	public MqttConnectOptions getConOptions() {
		return conOptions;
	}

	public Properties getDebug() {
		Properties props = new Properties();
		props.put("conState", new Integer(conState));
		props.put("serverURI", getClient().getServerURI());
		props.put("callback", callback);
		props.put("stoppingComms", new Boolean(stoppingComms));
		return props;
	}



	// Kick off the connect processing in the background so that it does not block. For instance
	// the socket could take time to create.
	private class ConnectBG implements Runnable {
		ClientComms 	clientComms = null;
		Thread 			cBg = null;
		MqttToken 		conToken;
		MqttConnect 	conPacket;

		ConnectBG(ClientComms cc, MqttToken cToken, MqttConnect cPacket) {
			clientComms = cc;
			conToken 	= cToken;
			conPacket 	= cPacket;
			cBg = new Thread(this, "MQTT Con: "+getClient().getClientId());
		}

		void start() {
			cBg.start();
		}

		public void run() {
			MqttException mqttEx = null;

			try {
				// Reset an exception on existing delivery tokens.
				// This will have been set if disconnect occured before delivery was
				// fully processed.
				MqttDeliveryToken[] toks = tokenStore.getOutstandingDelTokens();
				for (int i=0; i<toks.length; i++) {
					toks[i].internalTok.setException(null);
				}

				// Save the connect token in tokenStore as failure can occur before send
				tokenStore.saveToken(conToken,conPacket);

				// Connect to the server at the network level e.g. TCP socket and then
				// start the background processing threads before sending the connect
				// packet.
				NetworkModule networkModule = networkModules[networkModuleIndex];
				networkModule.start();
				receiver = new CommsReceiver(clientComms, clientState, tokenStore, networkModule.getInputStream());
				receiver.start("MQTT Rec: "+getClient().getClientId());
				sender = new CommsSender(clientComms, clientState, tokenStore, networkModule.getOutputStream());
				sender.start("MQTT Snd: "+getClient().getClientId());
				callback.start("MQTT Call: "+getClient().getClientId());				
				internalSend(conPacket, conToken);
			} catch (MqttException ex) {
				mqttEx = ex;
			} catch (Exception ex) {
				mqttEx =  ExceptionHelper.createMqttException(ex);
			}

			if (mqttEx != null) {
				shutdownConnection(conToken, mqttEx);
			}
		}
	}

	// Kick off the disconnect processing in the background so that it does not block. For instance
	// the quiesce
	private class DisconnectBG implements Runnable {
		Thread dBg = null;
		MqttDisconnect disconnect;
		long quiesceTimeout;
		MqttToken token;

		DisconnectBG(MqttDisconnect disconnect, long quiesceTimeout, MqttToken token ) {
			this.disconnect = disconnect;
			this.quiesceTimeout = quiesceTimeout;
			this.token = token;
		}

		void start() {
			dBg = new Thread(this, "MQTT Disc: "+getClient().getClientId());
			dBg.start();
		}
		
		public void run() {

			// Allow current inbound and outbound work to complete
			clientState.quiesce(quiesceTimeout);
			try {
				internalSend(disconnect, token);
				token.internalTok.waitUntilSent();
			}
			catch (MqttException ex) {
			}
			finally {
				token.internalTok.markComplete(null, null);
				shutdownConnection(token, null);
			}
		}
	}
	
	/*
	 * Check and send a ping if needed and check for ping timeout.
	 * Need to send a ping if nothing has been sent or received 
	 * in the last keepalive interval.
	 */
	public MqttToken checkForActivity(){
		MqttToken token = null;
		try{
			token = clientState.checkForActivity();
		}catch(MqttException e){
			handleRunException(e);
		}catch(Exception e){
			handleRunException(e);
		}
		return token;
	}	
	
	private void handleRunException(Exception ex) {
		MqttException mex;
		if ( !(ex instanceof MqttException)) {
			mex = new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, ex);
		} else {
			mex = (MqttException)ex;
		}

		shutdownConnection(null, mex);
	}
}
