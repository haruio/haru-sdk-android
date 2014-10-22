package com.haru;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;


import com.haru.push.MqttPushRoute;

public class PushService extends Service {
    // constants used to notify the Activity UI of received messages
    public static final String ACTION_PUSH_RECEIVED = "com.haru.push.RECEIVED";
    public static final String TOPIC_INTENT_EXTRA = "intent.extra.topic";

    // constants used to tell the Activity UI the connection status
    public static final String ACTION_PUSH_STATUS_INTENT = "com.haru.PUSH_STATUS";
    public static final String STATUS_INTENT_EXTRA = "intent.extra.status";

    // constant used internally to schedule the next ping event
    public static final String ACTION_PUSH_PING = "com.haru.PUSH_PING";

    public static final String ACTION_SUBSCRIBE_INTENT = "com.haru.SUBSCRIBE";
    public static final String ACTION_UNSUBSCRIBE_INTENT = "com.haru.UNSUBSCRIBE";
    public static final String CHANNEL_INTENT_EXTRA = "intent.extra.channel";

    private MqttPushRoute mqttPushRoute;


    /**
     * 서비스가 켜져있지 않을 시 서비스를 시작시킨다.
     * 부팅 완료시 (ACTION_BOOT_COMPLETED) 호출된다.
     * @param context
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

    /**
     * 서비스가 시작될 때 호출된다.
     */
    public void start(final Intent intent, final int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mqttPushRoute.handleStart(intent, startId);
            }
        }, "MQTTservice").start();

        // Sub / Unsub 브로드캐스트 리시버 등록
        Log.e("Haru", "Push service started!");
    }


    @Override
    public void onStart(final Intent intent, final int startId) {
        // This is the old onStart method that will be called on the pre-2.0
        // platform.  On 2.0 or later we override onStartCommand() so this
        // method will not be called.
        start(intent, startId);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        start(intent, startId);

        // return START_NOT_STICKY - we want this Service to be left running
        //  unless explicitly stopped, and it's process is killed, we want it to
        //  be restarted
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mqttPushRoute.serviceDestroyed();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mqttPushRoute.serviceBind();
    }
}