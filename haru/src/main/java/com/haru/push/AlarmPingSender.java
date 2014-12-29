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
 */
package com.haru.push;

import com.haru.push.mqtt.IMqttActionListener;
import com.haru.push.mqtt.IMqttToken;
import com.haru.push.mqtt.MqttPingSender;
import com.haru.push.mqtt.internal.ClientComms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 * <p/>
 * <p>This class implements the {@link MqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
class AlarmPingSender implements MqttPingSender {
    // Identifier for Intents, log messages, etc..
    static final String TAG = "AlarmPingSender";
    static final String PING_SENDER = "com.haru.mqtt.pingSender.";
    static final String PING_WAKELOCK = "com.haru.mqtt.client.";

    private ClientComms comms;
    private Context context;
    private BroadcastReceiver alarmReceiver;
    private AlarmPingSender that;
    private PendingIntent pendingIntent;
    private volatile boolean hasStarted = false;

    public AlarmPingSender(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }
        this.context = context;
        that = this;
    }

    @Override
    public void init(ClientComms comms) {
        this.comms = comms;
        this.alarmReceiver = new AlarmReceiver();
    }

    @Override
    public void start() {
        String action = PING_SENDER + comms.getClient().getClientId();
        Log.d(TAG, "Register alarmreceiver to MqttService" + action);
        context.registerReceiver(alarmReceiver, new IntentFilter(action));

        pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(action),
                PendingIntent.FLAG_UPDATE_CURRENT);

        schedule(comms.getKeepAlive());
        hasStarted = true;
    }

    @Override
    public void stop() {
        // Cancel Alarm.
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        Log.d(TAG, "Unregister alarmreceiver to MqttService" + comms.getClient().getClientId());
        if (hasStarted) {
            hasStarted = false;
            try {
                context.unregisterReceiver(alarmReceiver);
            } catch (IllegalArgumentException e) {
                //Ignore unregister errors.
            }
        }
    }

    @Override
    public void schedule(long delayInMilliseconds) {
        long nextAlarmInMilliseconds = System.currentTimeMillis()
                + delayInMilliseconds;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                pendingIntent);
    }

    /*
     * This class sends PingReq packet to MQTT broker
     */
    class AlarmReceiver extends BroadcastReceiver {
        private WakeLock wakelock;
        private String wakeLockTag = PING_WAKELOCK + that.comms.getClient().getClientId();

        @Override
        public void onReceive(Context context, Intent intent) {
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            // long as the alarm receiver's onReceive() method is executing.
            // This guarantees that the phone will not sleep until you have
            // finished handling the broadcast.", but this class still get
            // a wake lock to wait for ping finished.
            int count = intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, -1);
            Log.d(TAG, "Ping " + count + " times.");

            IMqttToken token = comms.checkForActivity();

            // No ping has been sent.
            if (token == null) {
                return;
            }

            // Assign new callback to token to execute code after PingResq
            // arrives. Get another wakelock even receiver already has one,
            // release it until ping response returns.
            if (wakelock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
                wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        wakeLockTag);
            }
            wakelock.acquire();
            token.setActionCallback(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Success. Release lock(" + wakeLockTag + "):"
                            + System.currentTimeMillis());
                    //Release wakelock when it is done.
                    if (wakelock != null && wakelock.isHeld()) {
                        wakelock.release();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Log.d(TAG, "Failure. Release lock(" + wakeLockTag + "):"
                            + System.currentTimeMillis());
                    //Release wakelock when it is done.
                    if (wakelock != null && wakelock.isHeld()) {
                        wakelock.release();
                    }
                }
            });
        }
    }
}
