package com.haru.test;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.haru.Config;
import com.haru.HaruFile;
import com.haru.User;
import com.haru.helpcenter.HelpCenter;
import com.haru.push.Push;
import com.haru.ui.helpcenter.HelpCenterActivity;
import com.haru.ui.helpcenter.SendQuestionDialogBuilder;
import com.haru.ui.login.LoginActivity;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Called when button is clicked.
     * This handler is set in res/styles.xml/MenuButtonStyle.
     *
     * @param view clicked button
     */
    public void onTestMenuClicked(View view) {
        switch (view.getId()) {

            case R.id.entity_test_btn:
                Intent intent = new Intent(this, EntityActivity.class);
                startActivity(intent);
                break;

            case R.id.user_test_btn:
                if (!User.isLogined()) {
                    Intent loginIntent = new Intent(this, LoginActivity.class);
                    startActivityForResult(loginIntent, LoginActivity.REQUEST_CODE);

                } else {
                    Intent userIntent = new Intent(this, UserActivity.class);
                    startActivity(userIntent);
                }
                break;

            case R.id.config_test_btn:
                configTest();
                break;

            case R.id.push_msg_btn:
                sendPushMsg();
                break;

            case R.id.push_noti_btn:
                sendPushNoti();
                break;

            case R.id.file_upload_sample_btn:
                if (!createSampleFile()) break;
                uploadSampleText();
                break;

            case R.id.helpcenter_faq_btn:
                helpCenterFaqList();
                break;

            case R.id.helpcenter_send_question_btn:
                helpCenterSendQuestion();
                break;

            case R.id.helpcenter_notice_btn:
                helpCenterNoticeList();
                break;

            case R.id.file_upload_image_btn:
                break;
        }
    }

    /**
     * Get Config.
     */
    void configTest() {
        Double version = Config.getDouble("appHello", -1.0);
        Toast.makeText(this, String.valueOf(version), Toast.LENGTH_SHORT).show();
    }

    /**
     * Called when login is finished.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LoginActivity.REQUEST_CODE) {
            // Go to user activity
            Intent intent = new Intent(this, UserActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Send push test message.
     */
    void sendPushMsg() {
        Push push = new Push.MessageBuilder()
                .setChannel("testChannel")
                .setMessage("Test Message Here!")
                .build();

        push.sendInBackground();
    }

    /**
     * Send push test message.
     */
    void sendPushNoti() {
        Push push = new Push.NotificationBuilder()
                .setChannel("testChannel")
                .setTitle("Test Notification")
                .setMessage("Hello? this is for test!")
                .build();

        push.sendInBackground();
    }

    /**
     * Upload sample file to server.
     */
    void uploadSampleText() {
        HaruFile file = new HaruFile(getFilesDir().getPath() + "/sample.txt");
        file.saveInBackground();
    }

    /**
     * Create sample.txt to upload to server.
     * @return Is it successful?
     */
    boolean createSampleFile() {
        File file = new File("sample.txt");
        if (file.exists()) return true;

        try {
            FileOutputStream writer = openFileOutput("sample.txt", Context.MODE_PRIVATE);
            for (int i=0;i<100;i++) {
                writer.write("hello world!".getBytes());
            }
            writer.close();

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Launch send question dialog.
     */
    void helpCenterSendQuestion() {
        new SendQuestionDialogBuilder(this).show();
    }

    /**
     * Launch Help Center Faq List activity.
     */
    void helpCenterFaqList() {
        Intent intent = new Intent(this, HelpCenterActivity.class);
        startActivity(intent);
    }

    /**
     * Launch Help Center Notice List activity.
     */
    void helpCenterNoticeList() {
        Intent intent = new Intent(this, HelpCenterNoticeActivity.class);
        startActivity(intent);
    }
}

