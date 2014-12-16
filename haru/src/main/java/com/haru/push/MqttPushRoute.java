package com.haru.push;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.haru.Haru;
import com.haru.Installation;
import com.haru.mqtt.IMqttActionListener;
import com.haru.mqtt.IMqttDeliveryToken;
import com.haru.mqtt.IMqttToken;
import com.haru.mqtt.MqttAsyncClient;
import com.haru.mqtt.MqttCallback;
import com.haru.mqtt.MqttClientPersistence;
import com.haru.mqtt.MqttConnectOptions;
import com.haru.mqtt.MqttException;
import com.haru.mqtt.MqttMessage;
import com.haru.mqtt.persist.MqttDefaultFilePersistence;

import java.io.File;

class MqttPushRoute implements MqttCallback {

    private static final String TAG = "HaruMQTT";

    // fields for the connection definition
    private static final String SERVER_URI = "tcp://push.haru.io:80";
    private String clientId = "Haru Push";

    private MqttClientPersistence persistence = null;

    // our client object - instantiated on connect
    private MqttAsyncClient myClient = null;

    private Context context;

    private volatile boolean disconnected = true;

    // Indicate this connection is connecting or not.
    // This variable uses to avoid reconnect multiple times.
    private volatile boolean isConnecting = false;

    private WakeLock wakelock = null;

    MqttPushRoute(Context context) {
        this.context = context;
    }

    /**
     * Connect to the server specified when we were instantiated
     */
    public void connect() {
        Haru.logD("Push: Connecting...");

        try {
            if (persistence == null) {
                // use internal storage as in-flight message persistence storage
                File myDir = context.getDir(TAG, Context.MODE_PRIVATE);

                if (myDir == null) {
                    // SHOULD NOT HAPPENED.
                    Haru.logE("Push: Error! No internal storage available");
                    return;
                }

                persistence = new MqttDefaultFilePersistence(myDir.getAbsolutePath());
            }

            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    doAfterConnectSuccess();
                    Haru.logD("Push: Connect success!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                    Haru.logD("Push: Connect Failed. reason : " + e.getMessage());
                    e.printStackTrace();
                    doAfterConnectFail();
                }
            };

            if (myClient != null) {
                if (!isConnecting && !disconnected) {
                    Haru.logD("Push: myClient != null and the client is connected and notify!");
                    doAfterConnectSuccess();

                } else {
                    Haru.logD("Push: myClient != null and the client is not connected");
                    Haru.logD("Push: Do Real connect!");
                    setConnectingState(true);
                    myClient.connect(null, null, listener);
                }
            }

            // if myClient is null, then create a new connection
            else {
                myClient = new MqttAsyncClient(SERVER_URI, clientId, persistence,
                        new AlarmPingSender(context));
                myClient.setCallback(this);

                Haru.logD("Push: Do Real connect!");
                setConnectingState(true);
                myClient.connect(new MqttConnectOptions(), null, listener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void doAfterConnectSuccess() {
        //since the device's cpu can go to sleep, acquire a wakelock and drop it later.
        acquireWakeLock();
        setConnectingState(false);
        disconnected = false;

        // subscribe to topic (appKey:installationId), QoS 2
        Installation installation = Installation.getCurrentInstallation();
        subscribe(Haru.getAppKey() + ":" + installation.getString("deviceToken"), 2);

        releaseWakeLock();
    }

    private void doAfterConnectFail() {
        acquireWakeLock();
        disconnected = true;
        setConnectingState(false);
        releaseWakeLock();
    }

    void disconnect() {
        Haru.logD("Push: disconnect()");
        disconnected = true;
        if ((myClient != null) && (myClient.isConnected())) {
            try {
                myClient.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Exception : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Log.e("disconnect", "not connected");
        }

        releaseWakeLock();
    }

    /**
     * Subscribe to a topic
     *
     * @param topic             a possibly wildcarded topic name
     * @param qos               requested quality of service for the topic
     */
    public void subscribe(final String topic, final int qos) {
        if ((myClient != null) && (myClient.isConnected())) {
            try {
                myClient.subscribe(topic, qos);
            } catch (Exception e) {
                Log.e(TAG, "Exception : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Log.e("subscribe", "not connected");
        }
    }

    /**
     * Subscribe to one or more topics
     *
     * @param topic a list of possibly wildcarded topic names
     * @param qos requested quality of service for each topic
     */
    public void subscribe(final String[] topic, final int[] qos) {
        if ((myClient != null) && (myClient.isConnected())) {
            try {
                myClient.subscribe(topic, qos);
            } catch (Exception e) {
                Log.e(TAG, "Exception : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Log.e("subscribe", "not connected");
        }
    }

    /**
     * Callback for connectionLost
     * @param why the exeception causing the break in communications
     */
    @Override
    public void connectionLost(Throwable why) {
        Haru.logD("Push: connectionLost(%s)", why.getMessage());
        disconnected = true;
        try {
            myClient.disconnect();
        } catch (Exception e) {
            // ignore it - we've done our best
        }

        // client has lost connection no need for wake lock
        releaseWakeLock();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken messageToken) {
        Haru.logD("Push: Delivery complete. (%s)", messageToken);
        // TODO: Delivered. WILL NOT USED IN CURRENT SDK VERSION
    }

    /**
     * Callback when a message is received
     * @param topic   the topic on which the message was received
     * @param message the message itself
     */
    @Override
    public void messageArrived(String topic, MqttMessage message)
            throws Exception {

        Haru.logI("Push: Arrived something! " + message.toString());
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(PushService.ACTION_PUSH_RECEIVED);
        broadcastIntent.putExtra(PushService.TOPIC_INTENT_EXTRA, topic);
        broadcastIntent.putExtra(Push.INTENT_EXTRA, Push.fromPacket(message.toString()));
        context.sendBroadcast(broadcastIntent);
    }

    /**
     * Acquires a partial wake lock for this client
     */
    private void acquireWakeLock() {
        if (wakelock == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
            wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, clientId);
        }
        wakelock.acquire();

    }

    /**
     * Releases the currently held wake lock for this client
     */
    private void releaseWakeLock() {
        if (wakelock != null && wakelock.isHeld()) {
            wakelock.release();
        }
    }

    /**
     * Receive notification that we are offline<br>
     * if cleanSession is true, we need to regard this as a disconnection
     */
    void offline() {
        if (!disconnected) {
            Exception e = new Exception("Android offline");
            connectionLost(e);
        }
    }

    /**
     * Reconnect<br>
     * Only appropriate if cleanSession is false and we were connected.
     * Declare as synchronized to avoid multiple calls to this method to send connect
     * multiple times
     */
    synchronized void reconnect() {
        if (isConnecting) {
            Haru.logD("Push: The client is connecting. Reconnect return directly.");
            return;
        }

        if (disconnected) {
            // use the activityToke the same with action connect
            Haru.logD("Push: Do Real Reconnect!");

            try {

                IMqttActionListener listener = new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // since the device's cpu can go to sleep, acquire a
                        // wakelock and drop it later.
                        Haru.logD("Push: Reconnect Success!");
                        Haru.logD("Push: DeliverBacklog when reconnect.");
                        doAfterConnectSuccess();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        doAfterConnectFail();
                    }
                };

                myClient.connect(null, null, listener);
                setConnectingState(true);

            } catch (MqttException e) {
                Log.e(TAG, "Cannot reconnect to remote server." + e.getMessage());
                setConnectingState(false);
            }
        }
    }

    synchronized void setConnectingState(boolean isConnecting) {
        this.isConnecting = isConnecting;
    }
}
