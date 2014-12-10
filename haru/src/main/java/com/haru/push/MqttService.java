/*******************************************************************************
 * Copyright (c) 1999, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package com.haru.push;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.haru.mqtt.IMqttDeliveryToken;
import com.haru.mqtt.MqttClientPersistence;
import com.haru.mqtt.MqttConnectOptions;
import com.haru.mqtt.MqttException;
import com.haru.mqtt.MqttMessage;
import com.haru.mqtt.MqttPersistenceException;
import com.haru.mqtt.MqttSecurityException;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class MqttService extends Service implements MqttTraceHandler {

    // Identifier for Intents, log messages, etc..
    static final String TAG = "MqttService";

    // callback id for making trace callbacks to the Activity
    // needs to be set by the activity as appropriate
    private String traceCallbackId;
    // state of tracing
    private boolean traceEnabled = false;

    // somewhere to persist received messages until we're sure
    // that they've reached the application
    MessageStore messageStore;

    // An intent receiver to deal with changes in network connectivity
    private NetworkConnectionIntentReceiver networkConnectionMonitor;

    //a receiver to recognise when the user changes the "background data" preference
    // and a flag to track that preference
    // Only really relevant below android version ICE_CREAM_SANDWICH - see
    // android docs
    private BackgroundDataPreferenceReceiver backgroundDataPreferenceMonitor;
    private volatile boolean backgroundDataEnabled = true;

    // mapping from client handle strings to actual client connections.
    private Map<String/* clientHandle */, MqttConnection/* client */> connections = new ConcurrentHashMap<String, MqttConnection>();

    public MqttService() {
        super();
    }

    /**
     * pass data back to the Activity, by building a suitable Intent object and
     * broadcasting it
     *
     * @param clientHandle source of the data
     * @param status       OK or Error
     * @param dataBundle   the data to be passed
     */
    void callbackToActivity(String clientHandle, Status status,
                            Bundle dataBundle) {
        // Don't call traceDebug, as it will try to callbackToActivity leading
        // to recursion.
        Intent callbackIntent = new Intent(
                MqttServiceConstants.CALLBACK_TO_ACTIVITY);
        if (clientHandle != null) {
            callbackIntent.putExtra(
                    MqttServiceConstants.CALLBACK_CLIENT_HANDLE, clientHandle);
        }
        callbackIntent.putExtra(MqttServiceConstants.CALLBACK_STATUS, status);
        if (dataBundle != null) {
            callbackIntent.putExtras(dataBundle);
        }
        sendBroadcast(callbackIntent);
    }

    // The major API implementation follows :-

    /**
     * Get an MqttConnection object to represent a connection to a server
     *
     * @param serverURI specifies the protocol, host name and port to be used to connect to an MQTT server
     * @param clientId  specifies the name by which this connection should be identified to the server
     * @param contextId specifies the app conext info to make a difference between apps
     * @return a string to be used by the Activity as a "handle" for this
     * MqttConnection
     */
    public String getClient(String serverURI, String clientId, String contextId, MqttClientPersistence persistence) {
        String clientHandle = serverURI + ":" + clientId + ":" + contextId;
        if (!connections.containsKey(clientHandle)) {
            MqttConnection client = new MqttConnection(this, serverURI,
                    clientId, persistence, clientHandle);
            connections.put(clientHandle, client);
        }
        return clientHandle;
    }

    /**
     * Connect to the MQTT server specified by a particular client
     *
     * @param clientHandle      identifies the MqttConnection to use
     * @param connectOptions    the MQTT connection options to be used
     * @param invocationContext arbitrary data to be passed back to the application
     * @param activityToken     arbitrary identifier to be passed back to the Activity
     * @throws MqttSecurityException
     * @throws MqttException
     */
    public void connect(String clientHandle, MqttConnectOptions connectOptions,
                        String invocationContext, String activityToken)
            throws MqttSecurityException, MqttException {
        MqttConnection client = getConnection(clientHandle);
        client.connect(connectOptions, invocationContext, activityToken);

    }

    /**
     * Request all clients to reconnect if appropriate
     */
    void reconnect() {
        traceDebug(TAG, "Reconnect to server, client size=" + connections.size());
        for (MqttConnection client : connections.values()) {
            traceDebug("Reconnect Client:",
                    client.getClientId() + '/' + client.getServerURI());
            if (this.isOnline()) {
                client.reconnect();
            }
        }
    }

    /**
     * Close connection from a particular client
     *
     * @param clientHandle identifies the MqttConnection to use
     */
    public void close(String clientHandle) {
        MqttConnection client = getConnection(clientHandle);
        client.close();
    }

    /**
     * Disconnect from the server
     *
     * @param clientHandle      identifies the MqttConnection to use
     * @param invocationContext arbitrary data to be passed back to the application
     * @param activityToken     arbitrary identifier to be passed back to the Activity
     */
    public void disconnect(String clientHandle, String invocationContext,
                           String activityToken) {
        MqttConnection client = getConnection(clientHandle);
        client.disconnect(invocationContext, activityToken);
        connections.remove(clientHandle);


        // the activity has finished using us, so we can stop the service
        // the activities are bound with BIND_AUTO_CREATE, so the service will
        // remain around until the last activity disconnects
        stopSelf();
    }

    /**
     * Disconnect from the server
     *
     * @param clientHandle      identifies the MqttConnection to use
     * @param quiesceTimeout    in milliseconds
     * @param invocationContext arbitrary data to be passed back to the application
     * @param activityToken     arbitrary identifier to be passed back to the Activity
     */
    public void disconnect(String clientHandle, long quiesceTimeout,
                           String invocationContext, String activityToken) {
        MqttConnection client = getConnection(clientHandle);
        client.disconnect(quiesceTimeout, invocationContext, activityToken);
        connections.remove(clientHandle);

        // the activity has finished using us, so we can stop the service
        // the activities are bound with BIND_AUTO_CREATE, so the service will
        // remain around until the last activity disconnects
        stopSelf();
    }

    /**
     * Get the status of a specific client
     *
     * @param clientHandle identifies the MqttConnection to use
     * @return true if the specified client is connected to an MQTT server
     */
    public boolean isConnected(String clientHandle) {
        MqttConnection client = getConnection(clientHandle);
        return client.isConnected();
    }

    /**
     * Publish a message to a topic
     *
     * @param clientHandle      identifies the MqttConnection to use
     * @param topic             the topic to which to publish
     * @param payload           the content of the message to publish
     * @param qos               the quality of service requested
     * @param retained          whether the MQTT server should retain this message
     * @param invocationContext arbitrary data to be passed back to the application
     * @param activityToken     arbitrary identifier to be passed back to the Activity
     * @return token for tracking the operation
     * @throws MqttPersistenceException
     * @throws MqttException
     */
    public IMqttDeliveryToken publish(String clientHandle, String topic,
                                      byte[] payload, int qos, boolean retained,
                                      String invocationContext, String activityToken)
            throws MqttPersistenceException, MqttException {
        MqttConnection client = getConnection(clientHandle);
        return client.publish(topic, payload, qos, retained, invocationContext,
                activityToken);
    }

    /**
     * Publish a message to a topic
     *
     * @param clientHandle      identifies the MqttConnection to use
     * @param topic             the topic to which to publish
     * @param message           the message to publish
     * @param invocationContext arbitrary data to be passed back to the application
     * @param activityToken     arbitrary identifier to be passed back to the Activity
     * @return token for tracking the operation
     * @throws MqttPersistenceException
     * @throws MqttException
     */
    public IMqttDeliveryToken publish(String clientHandle, String topic,
                                      MqttMessage message, String invocationContext, String activityToken)
            throws MqttPersistenceException, MqttException {
        MqttConnection client = getConnection(clientHandle);
        return client.publish(topic, message, invocationContext, activityToken);
    }

    /**
     * Subscribe to a topic
     *
     * @param clientHandle      identifies the MqttConnection to use
     * @param topic             a possibly wildcarded topic name
     * @param qos               requested quality of service for the topic
     * @param invocationContext arbitrary data to be passed back to the application
     * @param activityToken     arbitrary identifier to be passed back to the Activity
     */
    public void subscribe(String clientHandle, String topic, int qos,
                          String invocationContext, String activityToken) {
        MqttConnection client = getConnection(clientHandle);
        client.subscribe(topic, qos, invocationContext, activityToken);
    }

    /**
     * Subscribe to one or more topics
     *
     * @param clientHandle      identifies the MqttConnection to use
     * @param topic             a list of possibly wildcarded topic names
     * @param qos               requested quality of service for each topic
     * @param invocationContext arbitrary data to be passed back to the application
     * @param activityToken     arbitrary identifier to be passed back to the Activity
     */
    public void subscribe(String clientHandle, String[] topic, int[] qos,
                          String invocationContext, String activityToken) {
        MqttConnection client = getConnection(clientHandle);
        client.subscribe(topic, qos, invocationContext, activityToken);
    }

    /**
     * Unsubscribe from a topic
     *
     * @param clientHandle      identifies the MqttConnection
     * @param topic             a possibly wildcarded topic name
     * @param invocationContext arbitrary data to be passed back to the application
     * @param activityToken     arbitrary identifier to be passed back to the Activity
     */
    public void unsubscribe(String clientHandle, final String topic,
                            String invocationContext, String activityToken) {
        MqttConnection client = getConnection(clientHandle);
        client.unsubscribe(topic, invocationContext, activityToken);
    }

    /**
     * Unsubscribe from one or more topics
     *
     * @param clientHandle      identifies the MqttConnection
     * @param topic             a list of possibly wildcarded topic names
     * @param invocationContext arbitrary data to be passed back to the application
     * @param activityToken     arbitrary identifier to be passed back to the Activity
     */
    public void unsubscribe(String clientHandle, final String[] topic,
                            String invocationContext, String activityToken) {
        MqttConnection client = getConnection(clientHandle);
        client.unsubscribe(topic, invocationContext, activityToken);
    }

    /**
     * Get tokens for all outstanding deliveries for a client
     *
     * @param clientHandle identifies the MqttConnection
     * @return an array (possibly empty) of tokens
     */
    public IMqttDeliveryToken[] getPendingDeliveryTokens(String clientHandle) {
        MqttConnection client = getConnection(clientHandle);
        return client.getPendingDeliveryTokens();
    }

    /**
     * Get the MqttConnection identified by this client handle
     *
     * @param clientHandle identifies the MqttConnection
     * @return the MqttConnection identified by this handle
     */
    private MqttConnection getConnection(String clientHandle) {
        MqttConnection client = connections.get(clientHandle);
        if (client == null) {
            throw new IllegalArgumentException("Invalid ClientHandle");
        }
        return client;
    }

    /**
     * Called by the Activity when a message has been passed back to the
     * application
     *
     * @param clientHandle identifier for the client which received the message
     * @param id           identifier for the MQTT message
     */
    public Status acknowledgeMessageArrival(String clientHandle, String id) {
        if (messageStore.discardArrived(clientHandle, id)) {
            return Status.OK;
        } else {
            return Status.ERROR;
        }
    }

    // Extend Service

    /**
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        // disconnect immediately
        for (MqttConnection client : connections.values()) {
            client.disconnect(null, null);
        }

        unregisterBroadcastReceivers();

        if (this.messageStore != null)
            this.messageStore.close();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        // run till explicitly stopped, restart when
        // process restarted
        registerBroadcastReceivers();

        return START_STICKY;
    }

    /**
     * Identify the callbackId to be passed when making tracing calls back into
     * the Activity
     *
     * @param traceCallbackId identifier to the callback into the Activity
     */
    public void setTraceCallbackId(String traceCallbackId) {
        this.traceCallbackId = traceCallbackId;
    }

    /**
     * Turn tracing on and off
     *
     * @param traceEnabled set <code>true</code> to turn on tracing, <code>false</code> to turn off tracing
     */
    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    /**
     * Check whether trace is on or off.
     *
     * @return the state of trace
     */
    public boolean isTraceEnabled() {
        return this.traceEnabled;
    }

    /**
     * Trace debugging information
     *
     * @param tag     identifier for the source of the trace
     * @param message the text to be traced
     */
    @Override
    public void traceDebug(String tag, String message) {
        traceCallback(MqttServiceConstants.TRACE_DEBUG, tag, message);
//    Log.d(tag, message);
    }

    /**
     * Trace error information
     *
     * @param tag     identifier for the source of the trace
     * @param message the text to be traced
     */
    @Override
    public void traceError(String tag, String message) {
        traceCallback(MqttServiceConstants.TRACE_ERROR, tag, message);
    }

    private void traceCallback(String severity, String tag, String message) {
        if ((traceCallbackId != null) && (traceEnabled)) {
            Bundle dataBundle = new Bundle();
            dataBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.TRACE_ACTION);
            dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_SEVERITY, severity);
            dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_TAG, tag);
            //dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_ID, traceCallbackId);
            dataBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, message);
            callbackToActivity(traceCallbackId, Status.ERROR, dataBundle);
        }
    }

    /**
     * trace exceptions
     *
     * @param tag     identifier for the source of the trace
     * @param message the text to be traced
     * @param e       the exception
     */
    @Override
    public void traceException(String tag, String message, Exception e) {
        if (traceCallbackId != null) {
            Bundle dataBundle = new Bundle();
            dataBundle.putString(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.TRACE_ACTION);
            dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_SEVERITY, MqttServiceConstants.TRACE_EXCEPTION);
            dataBundle.putString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE, message);
            dataBundle.putSerializable(MqttServiceConstants.CALLBACK_EXCEPTION, e); //TODO: Check
            dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_TAG, tag);
            //dataBundle.putString(MqttServiceConstants.CALLBACK_TRACE_ID, traceCallbackId);
            callbackToActivity(traceCallbackId, Status.ERROR, dataBundle);
        }
    }

    @SuppressWarnings("deprecation")
    private void registerBroadcastReceivers() {
        if (networkConnectionMonitor == null) {
            networkConnectionMonitor = new NetworkConnectionIntentReceiver();
            registerReceiver(networkConnectionMonitor, new IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION));
        }

        if (Build.VERSION.SDK_INT < 14 /**Build.VERSION_CODES.ICE_CREAM_SANDWICH**/) {
            // Support the old system for background data preferences
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            backgroundDataEnabled = cm.getBackgroundDataSetting();
            if (backgroundDataPreferenceMonitor == null) {
                backgroundDataPreferenceMonitor = new BackgroundDataPreferenceReceiver();
                registerReceiver(
                        backgroundDataPreferenceMonitor,
                        new IntentFilter(
                                ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));
            }
        }
    }

    private void unregisterBroadcastReceivers() {
        if (networkConnectionMonitor != null) {
            unregisterReceiver(networkConnectionMonitor);
            networkConnectionMonitor = null;
        }

        if (Build.VERSION.SDK_INT < 14 /**Build.VERSION_CODES.ICE_CREAM_SANDWICH**/) {
            if (backgroundDataPreferenceMonitor != null) {
                unregisterReceiver(backgroundDataPreferenceMonitor);
            }
        }
    }

    /*
     * Called in response to a change in network connection - after losing a
     * connection to the server, this allows us to wait until we have a usable
     * data connection again
     */
    private class NetworkConnectionIntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            traceDebug(TAG, "Internal network status receive.");
            // we protect against the phone switching off
            // by requesting a wake lock - we request the minimum possible wake
            // lock - just enough to keep the CPU running until we've finished
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            WakeLock wl = pm
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();
            traceDebug(TAG, "Reconnect for Network recovery.");
            if (isOnline()) {
                traceDebug(TAG, "Online,reconnect.");
                // we have an internet connection - have another try at
                // connecting
                reconnect();
            } else {
                notifyClientsOffline();
            }

            wl.release();
        }
    }

    /**
     * @return whether the android service can be regarded as online
     */
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected()
                && backgroundDataEnabled) {
            return true;
        }

        return false;
    }

    /**
     * Notify clients we're offline
     */
    public void notifyClientsOffline() {
        for (MqttConnection connection : connections.values()) {
            connection.offline();
        }
    }

    /**
     * Detect changes of the Allow Background Data setting - only used below
     * ICE_CREAM_SANDWICH
     */
    private class BackgroundDataPreferenceReceiver extends BroadcastReceiver {

        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            traceDebug(TAG, "Reconnect since BroadcastReceiver.");
            if (cm.getBackgroundDataSetting()) {
                if (!backgroundDataEnabled) {
                    backgroundDataEnabled = true;
                    // we have the Internet connection - have another try at
                    // connecting
                    reconnect();
                }
            } else {
                backgroundDataEnabled = false;
                notifyClientsOffline();
            }
        }
    }

}
