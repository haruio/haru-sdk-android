package com.haru.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.haru.Installation;
import com.haru.User;

import java.text.SimpleDateFormat;

/**
 *
 */
public class UserActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // not logined?
        if (!User.isLogined()) {
            finish();
            return;
        }

        // get user
        User user = User.getCurrentUser();

        // get current installation
        Installation installation = Installation.getCurrentInstallation();

        // show current status
        String status = "Currently Logined as " + user.getUserName() + "\n" +
            "User created at : " + new SimpleDateFormat().format(user.getCreatedAt()) + "\n" +
            "Session-Token : " + User.getCurrentSessionToken() + "\n" +
            "Installation Id : " + installation.getId() + "\n\n" +
            "Raw user : " + user.toJson();
        TextView statusText = (TextView) findViewById(R.id.user_status_text);
        statusText.setText(status);

        // log out button
        Button logOutButton = (Button) findViewById(R.id.logout_btn);
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // log out!
                User.logOutInBackground();
                finish();
            }
        });
    }
}
