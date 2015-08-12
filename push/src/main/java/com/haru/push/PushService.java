package com.haru.push;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.haru.Haru;
import com.haru.Installation;

/**
 * 백그라운드에서 실행되면서 Push를 수신해서 AndroidManifest.xml에 지정된 PushReceiver를 호출한다. <br/>
 * <b>Push를 사용하기 위해서 반드시 이 서비스는 AndroidManifest.xml에 정의되어야만 한다!</b>
 */
public class PushService extends Service {
    // constants used to notify the Activity UI of received messages
    public static final String ACTION_PUSH_RECEIVED = "com.haru.push.RECEIVED";
    public static final String TOPIC_INTENT_EXTRA = "intent.extra.topic";

    // using MQTT - Haru currently does not support GCM (Google Cloud Messaging)
    private MqttPushRoute mqttPushRoute;

    private NetworkConnectionIntentReceiver networkConnectionMonitor;
    private BackgroundDataStatusReceiver backgroundDataStatusMonitor;
    private boolean backgroundDataEnabled = true;

    /**
     * Start the service if it's not started.
     * Called after device boots (ACTION_BOOT_COMPLETED)
     *
     * @param context Application Context
     */
    public static void startIfRequired(Context context) {

        // 서비스가 실행 중인지 서비스 목록을 체크한다.
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PushService.class.getName().equals(service.service.getClassName())) {
                // 서비스가 실행 중 : 그냥 return
                return;
            }
        }

        // 서비스 시작
        Intent intent = new Intent(context, PushService.class);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mqttPushRoute = new MqttPushRoute(this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStart(final Intent intent, final int startId) {
        // This is the old onStart method that will be called on the pre-2.0
        // platform.  On 2.0 or later we override onStartCommand() so this
        // method will not be called.
        start();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        start();

        // return START_NOT_STICKY - we want this Service to be left running
        //  unless explicitly stopped, and it's process is killed, we want it to
        //  be restarted
        return START_STICKY;
    }

    /**
     * Called when the PushService is started.
     */
    private void start() {
        registerBroadcastReceivers();
        mqttPushRoute.connect();

        Haru.logI("Push service started!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceivers();
        mqttPushRoute.disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // This service is not bindable.
    }

    @SuppressWarnings("deprecation")
    private void registerBroadcastReceivers() {
        if (networkConnectionMonitor == null) {
            networkConnectionMonitor = new NetworkConnectionIntentReceiver();
            registerReceiver(networkConnectionMonitor, new IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION));
        }

        if (Build.VERSION.SDK_INT < 14) {
            // Support the old system for background data preferences
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            backgroundDataEnabled = cm.getBackgroundDataSetting();
            if (backgroundDataStatusMonitor == null) {
                backgroundDataStatusMonitor = new BackgroundDataStatusReceiver();
                registerReceiver(backgroundDataStatusMonitor,
                        new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));
            }
        }
    }

    private void unregisterBroadcastReceivers(){
        if(networkConnectionMonitor != null){
            unregisterReceiver(networkConnectionMonitor);
            networkConnectionMonitor = null;
        }

        if (Build.VERSION.SDK_INT < 14) {
            if(backgroundDataStatusMonitor != null){
                unregisterReceiver(backgroundDataStatusMonitor);
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
            try {
                // we protect against the phone switching off
                // by requesting a wake lock - we request the minimum possible wake
                // lock - just enough to keep the CPU running until we've finished
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
                wl.acquire();

                mqttPushRoute.onNetworkStateChanged(isNetworkOnline());

                if (isNetworkOnline()) {
                    // we have an internet connection - have another try at
                    // connecting
                    mqttPushRoute.reconnect();
                } else mqttPushRoute.offline();
                wl.release();

            } catch (Exception e) {
                if (e != null) Haru.logE(e.getMessage());
            }
        }
    }

    /**
     * Detect changes of the Allow Background Data setting - only used below
     * ICE_CREAM_SANDWICH
     */
    private class BackgroundDataStatusReceiver extends BroadcastReceiver {

        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                Haru.logD("Push: Reconnect since background data is enabled.");
                if (cm.getBackgroundDataSetting()) {
                    if (!backgroundDataEnabled) {
                        // we have the Internet connection - have another try at reconnecting.
                        backgroundDataEnabled = true;
                        mqttPushRoute.onNetworkStateChanged(isNetworkOnline());
                        mqttPushRoute.reconnect();
                    }
                } else {
                    backgroundDataEnabled = false;
                    mqttPushRoute.onNetworkStateChanged(isNetworkOnline());
                    mqttPushRoute.offline();
                }
            } catch (Exception e) {
                if (e != null) Haru.logE(e.getMessage());
            }
        }
    }

    public boolean isNetworkOnline() throws Exception {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null
                && networkInfo.isAvailable()
                && networkInfo.isConnected()
                && backgroundDataEnabled;
    }
}