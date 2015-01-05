package com.haru.push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.haru.Haru;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Push를 수신받는 리시버이다.<br/>
 * 이 리시버와 {@link #onMessage(android.content.Context, Push)},
 * {@link #onNotification(android.content.Context, Push)} 함수를 상속받아 푸시가 왔을 때의 리시버를 만들 수 있다.
 *
 * <br/>
 * <b>푸시를 받기 위해서는 PushReceiver 혹은 상속받은 리시버가 반드시 AndroidManifest.xml에 등록되어야만 한다!</b>
 */
public class PushReceiver extends BroadcastReceiver {

    private final static AtomicInteger NOTI_ID = new AtomicInteger(0);

    /**
     * Push 관련 Broadcast를 받을 시 호출된다. <b>절대로 Override되어선 안된다.</b>
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(PushService.ACTION_PUSH_RECEIVED)) {

            Push push = intent.getParcelableExtra(Push.INTENT_EXTRA);
            if (push == null) return;

            // 기본 핸들러
            onPush(context, push);

        } else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {

            // 부팅 완료시 : 서비스를 실행시킨다.
            PushService.startIfRequired(context);
        }
    }

    /**
     * Notification이든, Message든 푸시가 오면 가장 먼저 호출된다.
     * 푸시의 종류를 분기해서 알맞은 핸들러 함수
     * ({@link #onMessage(Context, Push)}나 {@link #onNotification(Context, Push)}를 호출한다.
     *
     * @param context Application {@link android.content.Context}
     * @param push 푸시 데이터
     */
    public void onPush(Context context, Push push) {
        switch (push.getType()) {
            case Push.TYPE_MESSAGE:
                onMessage(context, push);
                break;

            case Push.TYPE_NOTIFICATION:
                // 핸들러에서 만든 알림을 띄운다.
                NotificationManager nm =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                Notification noti = onNotification(context, push);
                nm.notify(NOTI_ID.incrementAndGet(), noti);
        }
    }

    /**
     * 일반적인 Push Message를 받을 시 호출된다.
     *
     * @param context Application {@link android.content.Context}
     * @param push 푸시 데이터
     */
    public void onMessage(Context context, Push push) { }

    /**
     * Push Notification을 송신할 시 호출된다.
     * 사용자가 알림 형태를 커스텀할 수 있으며, 이를 상속하지 않을 시 기본 앱 아이콘으로 알림 센터 알림을 띄운다.
     *
     * @param context {@link android.content.Context}
     * @param push 푸시 데이터
     * @return Android 알림 센터에 올려질 {@link android.app.Notification} 객체.
     */
    public Notification onNotification(Context context, Push push) {
        Notification notification = new Notification(context.getApplicationInfo().icon,
                push.getTitle(),
                System.currentTimeMillis());

        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.ledARGB = Color.MAGENTA;

        Intent notificationIntent = new Intent(context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName()));
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(context, push.getTitle(), push.getMessage(), contentIntent);

        return notification;
    }
}
