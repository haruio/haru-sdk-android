package com.haru.test;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.haru.push.Push;
import com.haru.push.PushReceiver;

/**
 *
 */
public class TestPushReceiver extends PushReceiver {

    private static int NOTI_ID = 1000;

    @Override
    public Notification onNotification(Context context, Push push) {
        Log.d("HaruTest", "Noti Received  " + push.getMessage());
        return new NotificationCompat.Builder(context)
                .setTicker(push.getMessage())
                .setContentTitle(push.getTitle())
                .setContentText(push.getMessage())
                .setContentIntent(PendingIntent.getActivity(context, 0,
                        new Intent(context, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.drawable.ic_launcher)
                .build();
    }

    @Override
    public void onMessage(Context context, Push push) {
        Log.i("HaruTest", "Message Received => " + push.getMessage());
        if (push.getMessage().equals("lossTest")) return;
        Toast.makeText(context, push.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPush(Context context, Push push) {
        if (push.getMessage().equals("lossTest")) {
            Log.e("Haru", "LossTest Push Recv!");

            // notice to activity
            Intent intent = new Intent("com.haru.test.PushLossTest");
            context.sendBroadcast(intent);
        }
        super.onPush(context, push);
    }
}
