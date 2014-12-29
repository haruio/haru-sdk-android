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
 *    Ian Craggs - MQTT 3.1.1 support
 */
package com.haru.push.mqtt;

import com.haru.push.mqtt.internal.ClientComms;
import com.haru.push.mqtt.internal.ConnectActionListener;
import com.haru.push.mqtt.internal.ExceptionHelper;
import com.haru.push.mqtt.internal.LocalNetworkModule;
import com.haru.push.mqtt.internal.NetworkModule;
import com.haru.push.mqtt.internal.SSLNetworkModule;
import com.haru.push.mqtt.internal.TCPNetworkModule;
import com.haru.push.mqtt.internal.security.SSLSocketFactoryFactory;
import com.haru.push.mqtt.internal.wire.MqttDisconnect;
import com.haru.push.mqtt.internal.wire.MqttPublish;
import com.haru.push.mqtt.internal.wire.MqttSubscribe;
import com.haru.push.mqtt.internal.wire.MqttUnsubscribe;
import com.haru.push.mqtt.persist.MemoryPersistence;
import com.haru.push.mqtt.persist.MqttDefaultFilePersistence;

import java.util.Hashtable;
import java.util.Properties;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * Lightweight client for talking to an MQTT server using non-blocking methods
 * that allow an operation to run in the background.
 *
 * <p>This class implements the non-blocking {@link IMqttClient} client interface
 * allowing applications to initiate MQTT actions and then carry on working while the
 * MQTT action completes on a background thread.
 * This implementation is compatible with all Java SE runtimes from 1.4.2 and up.
 * </p>
 * <p>An application can connect to an MQTT server using:
 * <ul>
 * <li>A plain TCP socket
 * <li>A secure SSL/TLS socket
 * </ul>
 * </p>
 * <p>To enable messages to be delivered even across network and client restarts
 * messages need to be safely stored until the message has been delivered at the requested
 * quality of service. A pluggable persistence mechanism is provided to store the messages.
 * </p>
 * <p>By default {@link MqttDefaultFilePersistence} is used to store messages to a file.
 * If persistence is set to null then messages are stored in memory and hence can be lost
 * if the client, Java runtime or device shuts down.
 * </p>
 * <p>If connecting with {@link MqttConnectOptions#setCleanSession(boolean)} set to true it
 * is safe to use memory persistence as all state is cleared when a client disconnects. If
 * connecting with cleanSession set to false in order to provide reliable message delivery
 * then a persistent message store such as the default one should be used.
 * </p>
 * <p>The message store interface is pluggable. Different stores can be used by implementing
 * the {@link MqttClientPersistence} interface and passing it to the clients constructor.
 * </p>
 *
 * @see IMqttClient
 */
public class MqttClient implements IMqttClient { // DestinationProvider {
	private static final String CLIENT_ID_PREFIX = "paho";
	private static final long QUIESCE_TIMEOUT = 30000; // ms
	private static final long DISCONNECT_TIMEOUT = 10000; // ms
	private static final char MIN_HIGH_SURROGATE = '\uD800';
	private static final char MAX_HIGH_SURROGATE = '\uDBFF';
	private String clientId;
	private String serverURI;
	protected ClientComms comms;
	private Hashtable topics;
	private MqttClientPersistence persistence;

	/**
	 * Create an MqttAsyncClient that is used to communicate with an MQTT server.
	 * <p>
	 * The address of a server can be specified on the constructor. Alternatively
	 * a list containing one or more servers can be specified using the
	 * {@link MqttConnectOptions#setServerURIs(String[]) setServerURIs} method
	 * on MqttConnectOptions.
	 *
	 * <p>The <code>serverURI</code> parameter is typically used with the
	 * the <code>clientId</code> parameter to form a key. The key
	 * is used to store and reference messages while they are being delivered.
	 * Hence the serverURI specified on the constructor must still be specified even if a list
	 * of servers is specified on an MqttConnectOptions object.
	 * The serverURI on the constructor must remain the same across
	 * restarts of the client for delivery of messages to be maintained from a given
	 * client to a given server or set of servers.
	 *
	 * <p>The address of the server to connect to is specified as a URI. Two types of
	 * connection are supported <code>tcp://</code> for a TCP connection and
	 * <code>ssl://</code> for a TCP connection secured by SSL/TLS.
	 * For example:
	 * <ul>
	 * 	<li><code>tcp://localhost:1883</code></li>
	 * 	<li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * If the port is not specified, it will
	 * default to 1883 for <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that 65535 characters.
	 * It must be unique across all clients connecting to the same
	 * server. The clientId is used by the server to store data related to the client,
	 * hence it is important that the clientId remain the same when connecting to a server
	 * if durable subscriptions or reliable messaging are required.
	 * <p>A convenience method is provided to generate a random client id that
	 * should satisfy this criteria - {@link #generateClientId()}. As the client identifier
	 * is used by the server to identify a client when it reconnects, the client must use the
	 * same identifier between connections if durable subscriptions or reliable
	 * delivery of messages is required.
	 * </p>
	 * <p>
	 * In Java SE, SSL can be configured in one of several ways, which the
	 * client will use in the following order:
	 * </p>
	 * <ul>
	 * 	<li><strong>Supplying an <code>SSLSocketFactory</code></strong> - applications can
	 * use {@link MqttConnectOptions#setSocketFactory(javax.net.SocketFactory)} to supply
	 * a factory with the appropriate SSL settings.</li>
	 * 	<li><strong>SSL Properties</strong> - applications can supply SSL settings as a
	 * simple Java Properties using {@link MqttConnectOptions#setSSLProperties(java.util.Properties)}.</li>
	 * 	<li><strong>Use JVM settings</strong> - There are a number of standard
	 * Java system properties that can be used to configure key and trust stores.</li>
	 * </ul>
	 *
	 * <p>In Java ME, the platform settings are used for SSL connections.</p>
	 *
	 * <p>An instance of the default persistence mechanism {@link MqttDefaultFilePersistence}
	 * is used by the client. To specify a different persistence mechanism or to turn
	 * off persistence, use the {@link #MqttClient(String, String, MqttClientPersistence)}
	 * constructor.
	 *
	 * @param serverURI the address of the server to connect to, specified as a URI. Can be overridden using
	 * {@link MqttConnectOptions#setServerURIs(String[])}
	 * @param clientId a client identifier that is unique on the server being connected to
	 * @throws IllegalArgumentException if the URI does not start with
	 * "tcp://", "ssl://" or "local://".
	 * @throws IllegalArgumentException if the clientId is null or is greater than 65535 characters in length
	 * @throws MqttException if any other problem was encountered
	 */
	public MqttClient(String serverURI, String clientId) throws MqttException {
		this(serverURI,clientId, new MqttDefaultFilePersistence());
	}
	
	public MqttClient(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
		this(serverURI,clientId, persistence, new TimerPingSender());
	}

	/**
	 * Create an MqttAsyncClient that is used to communicate with an MQTT server.
	 * <p>
	 * The address of a server can be specified on the constructor. Alternatively
	 * a list containing one or more servers can be specified using the
	 * {@link MqttConnectOptions#setServerURIs(String[]) setServerURIs} method
	 * on MqttConnectOptions.
	 *
	 * <p>The <code>serverURI</code> parameter is typically used with the
	 * the <code>clientId</code> parameter to form a key. The key
	 * is used to store and reference messages while they are being delivered.
	 * Hence the serverURI specified on the constructor must still be specified even if a list
	 * of servers is specified on an MqttConnectOptions object.
	 * The serverURI on the constructor must remain the same across
	 * restarts of the client for delivery of messages to be maintained from a given
	 * client to a given server or set of servers.
	 *
	 * <p>The address of the server to connect to is specified as a URI. Two types of
	 * connection are supported <code>tcp://</code> for a TCP connection and
	 * <code>ssl://</code> for a TCP connection secured by SSL/TLS.
	 * For example:
	 * <ul>
	 * 	<li><code>tcp://localhost:1883</code></li>
	 * 	<li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * If the port is not specified, it will
	 * default to 1883 for <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that 65535 characters.
	 * It must be unique across all clients connecting to the same
	 * server. The clientId is used by the server to store data related to the client,
	 * hence it is important that the clientId remain the same when connecting to a server
	 * if durable subscriptions or reliable messaging are required.
	 * <p>A convenience method is provided to generate a random client id that
	 * should satisfy this criteria - {@link #generateClientId()}. As the client identifier
	 * is used by the server to identify a client when it reconnects, the client must use the
	 * same identifier between connections if durable subscriptions or reliable
	 * delivery of messages is required.
	 * </p>
	 * <p>
	 * In Java SE, SSL can be configured in one of several ways, which the
	 * client will use in the following order:
	 * </p>
	 * <ul>
	 * 	<li><strong>Supplying an <code>SSLSocketFactory</code></strong> - applications can
	 * use {@link MqttConnectOptions#setSocketFactory(javax.net.SocketFactory)} to supply
	 * a factory with the appropriate SSL settings.</li>
	 * 	<li><strong>SSL Properties</strong> - applications can supply SSL settings as a
	 * simple Java Properties using {@link MqttConnectOptions#setSSLProperties(java.util.Properties)}.</li>
	 * 	<li><strong>Use JVM settings</strong> - There are a number of standard
	 * Java system properties that can be used to configure key and trust stores.</li>
	 * </ul>
	 *
	 * <p>In Java ME, the platform settings are used for SSL connections.</p>
	 * <p>
	 * A persistence mechanism is used to enable reliable messaging.
	 * For messages sent at qualities of service (QoS) 1 or 2 to be reliably delivered,
	 * messages must be stored (on both the client and server) until the delivery of the message
	 * is complete. If messages are not safely stored when being delivered then
	 * a failure in the client or server can result in lost messages. A pluggable
	 * persistence mechanism is supported via the {@link MqttClientPersistence}
	 * interface. An implementer of this interface that safely stores messages
	 * must be specified in order for delivery of messages to be reliable. In
	 * addition {@link MqttConnectOptions#setCleanSession(boolean)} must be set
	 * to false. In the event that only QoS 0 messages are sent or received or
	 * cleanSession is set to true then a safe store is not needed.
	 * </p>
	 * <p>An implementation of file-based persistence is provided in
	 * class {@link MqttDefaultFilePersistence} which will work in all Java SE based
	 * systems. If no persistence is needed, the persistence parameter
	 * can be explicitly set to <code>null</code>.</p>
	 *
	 * @param serverURI the address of the server to connect to, specified as a URI. Can be overridden using
	 * {@link MqttConnectOptions#setServerURIs(String[])}
	 * @param clientId a client identifier that is unique on the server being connected to
 	 * @param persistence the persistence class to use to store in-flight message. If null then the
 	 * default persistence mechanism is used
	 * @throws IllegalArgumentException if the URI does not start with
	 * "tcp://", "ssl://" or "local://"
	 * @throws IllegalArgumentException if the clientId is null or is greater than 65535 characters in length
	 * @throws MqttException if any other problem was encountered
	 */
	public MqttClient(String serverURI, String clientId, MqttClientPersistence persistence, MqttPingSender pingSender) throws MqttException {

		if (clientId == null) { //Support empty client Id, 3.1.1 standard
			throw new IllegalArgumentException("Null clientId");
		}
		// Count characters, surrogate pairs count as one character.
		int clientIdLength = 0;
		for (int i = 0; i < clientId.length() - 1; i++) {
			if (Character_isHighSurrogate(clientId.charAt(i)))
				i++;
			clientIdLength++;
		}
		if ( clientIdLength > 65535) {
			throw new IllegalArgumentException("ClientId longer than 65535 characters");
		}

		MqttConnectOptions.validateURI(serverURI);

		this.serverURI = serverURI;
		this.clientId = clientId;

		this.persistence = persistence;
		if (this.persistence == null) {
			this.persistence = new MemoryPersistence();
		}


		this.persistence.open(clientId, serverURI);
		this.comms = new ClientComms(this, this.persistence, pingSender);
		this.persistence.close();
		this.topics = new Hashtable();

	}
	
	

	/**
	 * @param ch
	 * @return returns 'true' if the character is a high-surrogate code unit
	 */
	protected static boolean Character_isHighSurrogate(char ch) {
		return(ch >= MIN_HIGH_SURROGATE) && (ch <= MAX_HIGH_SURROGATE);
	}

	/**
	 * Factory method to create an array of network modules, one for
	 * each of the supplied URIs
	 *
	 * @param address the URI for the server.
	 * @return a network module appropriate to the specified address.
	 */

	// may need an array of these network modules

	protected NetworkModule[] createNetworkModules(String address, MqttConnectOptions options) throws MqttException, MqttSecurityException {

		NetworkModule[] networkModules = null;
		String[] serverURIs = options.getServerURIs();
		String[] array = null;
		if (serverURIs == null) {
			array = new String[]{address};
		} else if (serverURIs.length == 0) {
			array = new String[]{address};
		} else {
			array = serverURIs;
		}

		networkModules = new NetworkModule[array.length];
		for (int i = 0; i < array.length; i++) {
			networkModules[i] = createNetworkModule(array[i], options);
		}

		return networkModules;
	}

	/**
	 * Factory method to create the correct network module, based on the
	 * supplied address URI.
	 *
	 * @param address the URI for the server.
	 * @param Connect options
	 * @return a network module appropriate to the specified address.
	 */
	private NetworkModule createNetworkModule(String address, MqttConnectOptions options) throws MqttException, MqttSecurityException {

		NetworkModule netModule;
		String shortAddress;
		String host;
		int port;
		SocketFactory factory = options.getSocketFactory();

		int serverURIType = MqttConnectOptions.validateURI(address);

		switch (serverURIType) {
		case MqttConnectOptions.URI_TYPE_TCP :
			shortAddress = address.substring(6);
			host = getHostName(shortAddress);
			port = getPort(shortAddress, 1883);
			if (factory == null) {
				factory = SocketFactory.getDefault();
			}
			else if (factory instanceof SSLSocketFactory) {
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
			}
			netModule = new TCPNetworkModule(factory, host, port, clientId);
			((TCPNetworkModule)netModule).setConnectTimeout(options.getConnectionTimeout());
			break;
		case MqttConnectOptions.URI_TYPE_SSL:
			shortAddress = address.substring(6);
			host = getHostName(shortAddress);
			port = getPort(shortAddress, 8883);
			SSLSocketFactoryFactory factoryFactory = null;
			if (factory == null) {
//				try {
					factoryFactory = new SSLSocketFactoryFactory();
					Properties sslClientProps = options.getSSLProperties();
					if (null != sslClientProps)
						factoryFactory.initialize(sslClientProps, null);
					factory = factoryFactory.createSocketFactory(null);
//				}
//				catch (MqttDirectException ex) {
//					throw ExceptionHelper.createMqttException(ex.getCause());
//				}
			}
			else if ((factory instanceof SSLSocketFactory) == false) {
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
			}

			// Create the network module...
			netModule = new SSLNetworkModule((SSLSocketFactory) factory, host, port, clientId);
			((SSLNetworkModule)netModule).setSSLhandshakeTimeout(options.getConnectionTimeout());
			// Ciphers suites need to be set, if they are available
			if (factoryFactory != null) {
				String[] enabledCiphers = factoryFactory.getEnabledCipherSuites(null);
				if (enabledCiphers != null) {
					((SSLNetworkModule) netModule).setEnabledCiphers(enabledCiphers);
				}
			}
			break;
		case MqttConnectOptions.URI_TYPE_LOCAL :
			netModule = new LocalNetworkModule(address.substring(8));
			break;
		default:
			// This shouldn't happen, as long as validateURI() has been called.
			netModule = null;
		}
		return netModule;
	}

	private int getPort(String uri, int defaultPort) {
		int port;
		int portIndex = uri.lastIndexOf(':');
		if (portIndex == -1) {
			port = defaultPort;
		}
		else {
		    port = Integer.parseInt(uri.substring(portIndex + 1));
		}
		return port;
	}

	private String getHostName(String uri) {
		int schemeIndex = uri.lastIndexOf('/');
		int portIndex = uri.lastIndexOf(':');
		if (portIndex == -1) {
			portIndex = uri.length();
		}
		return uri.substring(schemeIndex + 1, portIndex);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#connect(java.lang.Object, IMqttActionListener)
	 */
	public IMqttToken connect(Object userContext, IMqttActionListener callback)
			throws MqttException, MqttSecurityException {
		return this.connect(new MqttConnectOptions(), userContext, callback);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#connect()
	 */
	public IMqttToken connect() throws MqttException, MqttSecurityException {
		return this.connect(null, null);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#connect(MqttConnectOptions)
	 */
	public IMqttToken connect(MqttConnectOptions options) throws MqttException, MqttSecurityException {
		return this.connect(options, null,null);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#connect(MqttConnectOptions, java.lang.Object, IMqttActionListener)
	 */
	public IMqttToken connect(MqttConnectOptions options, Object userContext, IMqttActionListener callback)
			throws MqttException, MqttSecurityException {
		if (comms.isConnected()) {
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_CONNECTED);
		}
		if (comms.isConnecting()) {
			throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
		}
		if (comms.isDisconnecting()) {
			throw new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
		}
		if (comms.isClosed()) {
			throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
		}

		comms.setNetworkModules(createNetworkModules(serverURI, options));

		// Insert our own callback to iterate through the URIs till the connect succeeds
		MqttToken userToken = new MqttToken(getClientId());
		ConnectActionListener connectActionListener = new ConnectActionListener(this, persistence, comms, options, userToken, userContext, callback);
		userToken.setActionCallback(connectActionListener);
		userToken.setUserContext(this);

		comms.setNetworkModuleIndex(0);
		connectActionListener.connect();

		return userToken;
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#disconnect(java.lang.Object, IMqttActionListener)
	 */
	public IMqttToken disconnect( Object userContext, IMqttActionListener callback) throws MqttException {
		return this.disconnect(QUIESCE_TIMEOUT, userContext, callback);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#disconnect()
	 */
	public IMqttToken disconnect() throws MqttException {
		return this.disconnect(null, null);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#disconnect(long)
	 */
	public IMqttToken disconnect(long quiesceTimeout) throws MqttException {
		return this.disconnect(quiesceTimeout, null, null);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#disconnect(long, java.lang.Object, IMqttActionListener)
	 */
	public IMqttToken disconnect(long quiesceTimeout, Object userContext, IMqttActionListener callback) throws MqttException {

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);

		MqttDisconnect disconnect = new MqttDisconnect();
		try {
			comms.disconnect(disconnect, quiesceTimeout, token);
		} catch (MqttException ex) {
			throw ex;
		}

		return token;
	}
	
	/*
	 * (non-Javadoc)
	 * @see IMqttAsyncClient#disconnectForcibly()
	 */
	public void disconnectForcibly() throws MqttException {
		disconnectForcibly(QUIESCE_TIMEOUT, DISCONNECT_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * @see IMqttAsyncClient#disconnectForcibly(long)
	 */
	public void disconnectForcibly(long disconnectTimeout) throws MqttException {
		disconnectForcibly(QUIESCE_TIMEOUT, disconnectTimeout);
	}
	
	/*
	 * (non-Javadoc)
	 * @see IMqttAsyncClient#disconnectForcibly(long, long)
	 */
	public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException{
		comms.disconnectForcibly(quiesceTimeout, disconnectTimeout);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#isConnected()
	 */
	public boolean isConnected() {
		return comms.isConnected();
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#getClientId()
	 */
	public String getClientId() {
		return clientId;
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#getServerURI()
	 */
	public String getServerURI() {
		return serverURI;
	}

	/**
	 * Get a topic object which can be used to publish messages.
	 * <p>There are two alternative methods that should be used in preference to this one when publishing a message:
	 * <ul>
	 * <li>{@link MqttClient#publish(String, MqttMessage, MqttDeliveryToken)} to publish a message in a non-blocking manner or
	 * <li>{@link MqttClient#publishBlock(String, MqttMessage, MqttDeliveryToken)} to publish a message in a blocking manner
	 * </ul>
	 * </p>
	 * <p>When you build an application,
	 * the design of the topic tree should take into account the following principles
	 * of topic name syntax and semantics:</p>
	 *
	 * <ul>
	 * 	<li>A topic must be at least one character long.</li>
	 * 	<li>Topic names are case sensitive.  For example, <em>ACCOUNTS</em> and <em>Accounts</em> are
	 * 	two different topics.</li>
	 * 	<li>Topic names can include the space character.  For example, <em>Accounts
	 * 	payable</em> is a valid topic.</li>
	 * 	<li>A leading "/" creates a distinct topic.  For example, <em>/finance</em> is
	 * 	different from <em>finance</em>. <em>/finance</em> matches "+/+" and "/+", but
	 * 	not "+".</li>
	 * 	<li>Do not include the null character (Unicode<samp class="codeph"> \x0000</samp>) in
	 * 	any topic.</li>
	 * </ul>
	 *
	 * <p>The following principles apply to the construction and content of a topic
	 * tree:</p>
	 *
	 * <ul>
	 * 	<li>The length is limited to 64k but within that there are no limits to the
	 * 	number of levels in a topic tree.</li>
	 * 	<li>There can be any number of root nodes; that is, there can be any number
	 * 	of topic trees.</li>
	 * 	</ul>
	 * </p>
	 *
	 * @param topic the topic to use, for example "finance/stock/ibm".
	 * @return an MqttTopic object, which can be used to publish messages to
	 * the topic.
	 * @throws IllegalArgumentException if the topic contains a '+' or '#'
	 * wildcard character.
	 */
	protected MqttTopic getTopic(String topic) {
		MqttTopic.validate(topic, false/*wildcards NOT allowed*/);

		MqttTopic result = (MqttTopic)topics.get(topic);
		if (result == null) {
			result = new MqttTopic(topic, comms);
			topics.put(topic,result);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * Check and send a ping if needed.
	 * <p>By default, client sends PingReq to server to keep the connection to 
	 * server. For some platforms which cannot use this mechanism, such as Android,
	 * developer needs to handle the ping request manually with this method.
	 * </p>
	 * 
	 * @throws MqttException for other errors encountered while publishing the message.
	 */
	public IMqttToken checkPing(Object userContext, IMqttActionListener callback) throws MqttException{
		MqttToken token;
		
		token = comms.checkForActivity();
		
		return token;
	}
	
	
	/* (non-Javadoc)
	 * @see IMqttAsyncClient#subscribe(java.lang.String, int, java.lang.Object, IMqttActionListener)
	 */
	public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback) throws MqttException {
		return this.subscribe(new String[] {topicFilter}, new int[] {qos}, userContext, callback);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#subscribe(java.lang.String, int)
	 */
	public IMqttToken subscribe(String topicFilter, int qos) throws MqttException {
		return this.subscribe(new String[] {topicFilter}, new int[] {qos}, null, null);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#subscribe(java.lang.String[], int[])
	 */
	public IMqttToken subscribe(String[] topicFilters, int[] qos) throws MqttException {
		return this.subscribe(topicFilters, qos, null, null);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#subscribe(java.lang.String[], int[], java.lang.Object, IMqttActionListener)
	 */
	public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext, IMqttActionListener callback) throws MqttException {

		if (topicFilters.length != qos.length) {
			throw new IllegalArgumentException();
		}
		
		String subs = "";
		for (int i=0;i<topicFilters.length;i++) {
			if (i>0) {
				subs+=", ";
			}
			subs+= "topic="+ topicFilters[i]+" qos="+qos[i];
			
			//Check if the topic filter is valid before subscribing
			MqttTopic.validate(topicFilters[i], true/*allow wildcards*/);
		}

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.internalTok.setTopics(topicFilters);

		MqttSubscribe register = new MqttSubscribe(topicFilters, qos);

		comms.sendNoWait(register, token);

		return token;
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#unsubscribe(java.lang.String, java.lang.Object, IMqttActionListener)
	 */
	public IMqttToken unsubscribe(String topicFilter,  Object userContext, IMqttActionListener callback) throws MqttException {
		return unsubscribe(new String[] {topicFilter}, userContext, callback);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#unsubscribe(java.lang.String)
	 */
	public IMqttToken unsubscribe(String topicFilter) throws MqttException {
		return unsubscribe(new String[] {topicFilter}, null, null);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#unsubscribe(java.lang.String[])
	 */
	public IMqttToken unsubscribe(String[] topicFilters) throws MqttException {
		return unsubscribe(topicFilters, null, null);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#unsubscribe(java.lang.String[], java.lang.Object, IMqttActionListener)
	 */
	public IMqttToken unsubscribe(String[] topicFilters, Object userContext, IMqttActionListener callback) throws MqttException {
		String subs = "";
		for (int i=0;i<topicFilters.length;i++) {
			if (i>0) {
				subs+=", ";
			}
			subs+=topicFilters[i];
			
			// Check if the topic filter is valid before unsubscribing
			// Although we already checked when subscribing, but invalid
			// topic filter is meanless for unsubscribing, just prohibit it
			// to reduce unnecessary control packet send to broker.
			MqttTopic.validate(topicFilters[i], true/*allow wildcards*/);
		}
		

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.internalTok.setTopics(topicFilters);

		MqttUnsubscribe unregister = new MqttUnsubscribe(topicFilters);

		comms.sendNoWait(unregister, token);

		return token;
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#setCallback(MqttCallback)
	 */
	public void setCallback(MqttCallback callback) {
		comms.setCallback(callback);
	}

	/**
	 * Returns a randomly generated client identifier based on the the fixed prefix (paho)
	 * and the system time.
	 * <p>When cleanSession is set to false, an application must ensure it uses the
	 * same client identifier when it reconnects to the server to resume state and maintain
	 * assured message delivery.</p>
	 * @return a generated client identifier
	 * @see MqttConnectOptions#setCleanSession(boolean)
	 */
	public static String generateClientId() {
		//length of nanoTime = 15, so total length = 19  < 65535(defined in spec) 
		return CLIENT_ID_PREFIX + System.nanoTime();
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#getPendingDeliveryTokens()
	 */
	public IMqttDeliveryToken[] getPendingDeliveryTokens() {
		return comms.getPendingDeliveryTokens();
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#publish(java.lang.String, byte[], int, boolean, java.lang.Object, IMqttActionListener)
	 */
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos,
			boolean retained, Object userContext, IMqttActionListener callback) throws MqttException,
			MqttPersistenceException {
		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		message.setRetained(retained);
		return this.publish(topic, message, userContext, callback);
	}
	/* (non-Javadoc)
	 * @see IMqttAsyncClient#publish(java.lang.String, byte[], int, boolean)
	 */
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos,
			boolean retained) throws MqttException, MqttPersistenceException {
		return this.publish(topic, payload, qos, retained, null, null);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#publish(java.lang.String, MqttMessage)
	 */
	public IMqttDeliveryToken publish(String topic, MqttMessage message) throws MqttException, 	MqttPersistenceException {
		return this.publish(topic, message, null, null);
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#publish(java.lang.String, MqttMessage, java.lang.Object, IMqttActionListener)
	 */
	public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext, IMqttActionListener callback) throws MqttException,
			MqttPersistenceException {

		//Checks if a topic is valid when publishing a message.
		MqttTopic.validate(topic, false/*wildcards NOT allowed*/);

		MqttDeliveryToken token = new MqttDeliveryToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.setMessage(message);
		token.internalTok.setTopics(new String[] {topic});

		MqttPublish pubMsg = new MqttPublish(topic, message);
		comms.sendNoWait(pubMsg, token);


		return token;
	}

	/* (non-Javadoc)
	 * @see IMqttAsyncClient#close()
	 */
	public void close() throws MqttException {
		comms.close();

	}
}
