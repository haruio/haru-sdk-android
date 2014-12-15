package com.haru.test;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.haru.push.Push;

/**
 * Activity for testing push loss percentage.
 */
public class PushLossTestActivity extends Activity implements View.OnClickListener {

    private RadioGroup radioGroup;
    private TextView sentStatus, recvStatus;
    private int sent = 0, recv = 0;

    /**
     * loss testing thread.
     */
    private Runnable testRunnable = new Runnable() {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Push push;
                if (radioGroup.getCheckedRadioButtonId() == R.id.pushTypeRadioMessage) {
                    push = new Push.MessageBuilder()
                            .setMessage("lossTest")
                            .setChannel("testChannel")
                            .build();

                } else push = new Push.NotificationBuilder()
                        .setTitle("Loss Test!")
                        .setMessage("lossTest")
                        .setChannel("testChannel")
                        .build();

                push.sendInBackground();
                increaseSent();
                SystemClock.sleep(200);
            }
            testThread = null;
        }
    };
    private Thread testThread;

    /**
     * receiver that handles loss testing push messages.
     */
    private BroadcastReceiver pushReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("Haru", " => Okay, push received from activity.");
            increaseRecv();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_loss);

        Button startButton, stopButton, clearButton;
        startButton = (Button) findViewById(R.id.pushTestStart);
        stopButton = (Button) findViewById(R.id.pushTestEnd);
        clearButton = (Button) findViewById(R.id.pushTestClear);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);

        radioGroup = (RadioGroup) findViewById(R.id.pushTypeRadioGroup);
        sentStatus = (TextView) findViewById(R.id.pushStatusSent);
        recvStatus = (TextView) findViewById(R.id.pushStatusReceive);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(pushReceiver, new IntentFilter("com.haru.test.PushLossTest"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(pushReceiver);
    }

    private void increaseSent() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sentStatus.setText(String.format("Sent : %d", ++sent));
            }
        });
    }

    private void increaseRecv() {
        recvStatus.setText(String.format("Recieved : %d (%.1f percent loss)", ++recv,
                (float) (sent - recv) / sent * 100));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.pushTestStart:
                testThread = new Thread(testRunnable);
                testThread.start();
                break;

            case R.id.pushTestEnd:
                if (testThread != null) {
                    testThread.interrupt();
                    testThread = null;
                }
                break;

            case R.id.pushTestClear:
                sent = 0;
                recv = 0;
                sentStatus.setText("");
                recvStatus.setText("");
        }
    }
}
