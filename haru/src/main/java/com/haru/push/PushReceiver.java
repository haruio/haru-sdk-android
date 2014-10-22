package com.haru.push;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
            Log.e("Haru", "push Received! " + push.getMessage());

            switch (push.getType()) {
                case Push.TYPE_MESSAGE:
                    onMessage(context, push);
                    break;

                case Push.TYPE_NOTIFICATION:
                    // 핸들러에서 만든 알림을 띄운다.
                    Notification noti = onNotification(context, push);
                    noti.notify();
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

        return noti;
    }
}
