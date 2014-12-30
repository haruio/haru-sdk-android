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
package com.haru.push.mqtt.internal;

import com.haru.push.mqtt.MqttException;
import com.haru.push.mqtt.MqttToken;
import com.haru.push.mqtt.internal.wire.MqttAck;
import com.haru.push.mqtt.internal.wire.MqttInputStream;
import com.haru.push.mqtt.internal.wire.MqttWireMessage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Receives MQTT packets from the server.
 */
public class CommsReceiver implements Runnable {
	private boolean running = false;
	private Object lifecycle = new Object();
	private ClientState clientState = null;
	private ClientComms clientComms = null;
	private MqttInputStream in;
	private CommsTokenStore tokenStore = null;
	private Thread recThread = null;
	private volatile boolean receiving;
	
	public CommsReceiver(ClientComms clientComms, ClientState clientState,CommsTokenStore tokenStore, InputStream in) {
		this.in = new MqttInputStream(clientState, in);
		this.clientComms = clientComms;
		this.clientState = clientState;
		this.tokenStore = tokenStore;
	}
	
	/**
	 * Starts up the Receiver's thread.
	 */
	public void start(String threadName) {
		synchronized (lifecycle) {
			if (!running) {
				running = true;
				recThread = new Thread(this, threadName);
				recThread.start();
			}
		}
	}

	/**
	 * Stops the Receiver's thread.  This call will block.
	 */
	public void stop() {
		synchronized (lifecycle) {
			if (running) {
				running = false;
				receiving = false;
				if (!Thread.currentThread().equals(recThread)) {
					try {
						// Wait for the thread to finish.
						recThread.join();
					}
					catch (InterruptedException ex) {
					}
				}
			}
		}
		recThread = null;
	}
	
	/**
	 * Run loop to receive messages from the server.
	 */
	public void run() {
		MqttToken token = null;
		
		while (running && (in != null)) {
			try {
				receiving = in.available() > 0;
				MqttWireMessage message = in.readMqttWireMessage();
				receiving = false;
				
				if (message instanceof MqttAck) {
					token = tokenStore.getToken(message);
					if (token!=null) {
						synchronized (token) {
							// Ensure the notify processing is done under a lock on the token
							// This ensures that the send processing can complete  before the 
							// receive processing starts! ( request and ack and ack processing
							// can occur before request processing is complete if not!
							clientState.notifyReceivedAck((MqttAck)message);
						}
					} else {
						// It its an ack and there is no token then something is not right.
						// An ack should always have a token assoicated with it.
						throw new MqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
					}
				} else {
					// A new message has arrived
					clientState.notifyReceivedMsg(message);
				}
			}
			catch (MqttException ex) {
				running = false;
				// Token maybe null but that is handled in shutdown
				clientComms.shutdownConnection(token, ex);
			} 
			catch (IOException ioe) {
				running = false;
				// An EOFException could be raised if the broker processes the 
				// DISCONNECT and ends the socket before we complete. As such,
				// only shutdown the connection if we're not already shutting down.
				if (!clientComms.isDisconnecting()) {
					clientComms.shutdownConnection(token, new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, ioe));
				}
			}
			finally {
				receiving = false;
			}
		}
	}
	
	public boolean isRunning() {
		return running;
	}
	
	/**
	 * Returns the receiving state.
	 * 
	 * @return true if the receiver is receiving data, false otherwise.
	 */
	public boolean isReceiving() {
		return receiving;
	}
}
