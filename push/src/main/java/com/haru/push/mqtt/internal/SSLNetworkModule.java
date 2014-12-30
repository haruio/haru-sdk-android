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

import java.io.IOException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * A network module for connecting over SSL.
 */
public class SSLNetworkModule extends TCPNetworkModule {
	private String[] enabledCiphers;
	private int handshakeTimeoutSecs;

	/**
	 * Constructs a new SSLNetworkModule using the specified host and
	 * port.  The supplied SSLSocketFactory is used to supply the network
	 * socket.
	 */
	public SSLNetworkModule(SSLSocketFactory factory, String host, int port, String resourceContext) {
		super(factory, host, port, resourceContext);
	}

	/**
	 * Returns the enabled cipher suites.
	 */
	public String[] getEnabledCiphers() {
		return enabledCiphers;
	}

	/**
	 * Sets the enabled cipher suites on the underlying network socket.
	 */
	public void setEnabledCiphers(String[] enabledCiphers) {
		this.enabledCiphers = enabledCiphers;
		if ((socket != null) && (enabledCiphers != null)) {
			((SSLSocket) socket).setEnabledCipherSuites(enabledCiphers);
		}
	}


	public void setSSLhandshakeTimeout(int timeout) {
		super.setConnectTimeout(timeout);
		this.handshakeTimeoutSecs = timeout;
	}
	
	public void start() throws IOException, MqttException {
		super.start();
		setEnabledCiphers(enabledCiphers);
		int soTimeout = socket.getSoTimeout();
		if ( soTimeout == 0 ) {
			// RTC 765: Set a timeout to avoid the SSL handshake being blocked indefinitely
			socket.setSoTimeout(this.handshakeTimeoutSecs*1000);
		}
		((SSLSocket)socket).startHandshake();
		// reset timeout to default value
		socket.setSoTimeout(soTimeout);   
	}
}
