package com.haru.push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import com.haru.Haru;
import com.haru.PushService;

/**
 * Push를 수신받는 리시버이다.
 */
public class PushReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(PushService.ACTION_PUSH_RECEIVED)) {

            Push push = intent.getParcelableExtra(Push.INTENT_EXTRA);
            if (push == null) return;

            Haru.logI("Push Received! " + push.getMessage());

            switch (push.getType()) {
                case Push.TYPE_MESSAGE:
                    onMessage(context, push);
                    break;

                case Push.TYPE_NOTIFICATION:
                    // 핸들러에서 만든 알림을 띄운다.
                    NotificationManager nm =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    Notification noti = onNotification(context, push);
                    nm.notify(3, noti);
            }

            // 기본 핸들러
            onPush(context, push);

        } else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {

            // 부팅 완료시 : 서비스를 실행시킨다.
            PushService.startIfRequired(context);
        }
    }

    public void onPush(Context context, Push push) {

    }

    public void onMessage(Context context, Push push) {
    }

    public Notification onNotification(Context context, Push push) {
        Notification noti =
                new Notification(context.getApplicationInfo().icon,
                        push.getTitle(), System.currentTimeMillis());

        Notification notification = new Notification(context.getApplicationInfo().icon,
                push.getTitle(),
                System.currentTimeMillis());

        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.ledARGB = Color.MAGENTA;

        Intent notificationIntent = new Intent("com.asdf");
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(context, push.getTitle(), push.getMessage(), contentIntent);

        return noti;
    }
}
