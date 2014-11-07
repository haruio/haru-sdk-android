package com.haru.examplememo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.haru.HaruException;
import com.haru.User;
import com.haru.callback.LoginCallback;
import com.haru.social.FacebookLoginUtils;
import com.haru.social.KakaoLoginUtils;
import com.kakao.Session;
import com.kakao.SessionCallback;
import com.kakao.exception.KakaoException;

/**
 * 샘플 메모앱 using Haru
 */
public class EmailLoginActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email_login);

        final EditText usernameInput = (EditText) findViewById(R.id.usernameInput),
                pwInput = (EditText) findViewById(R.id.passwordInput);
        Button loginButton = (Button) findViewById(R.id.logInButton),
                signInButton = (Button) findViewById(R.id.signInButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = usernameInput.getText().toString();
                String password = pwInput.getText().toString();

                User.logInInBackground(userName, password, new LoginCallback() {
                    @Override
                    public void done(User user, HaruException error) {
                        if (error != null) {
                            if (error.getErrorCode() == HaruException.USERNAME_MISSING) {
                            }
                        }

                        // Login succeed. Go to main activity
                        Intent intent = new Intent(EmailLoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
            }
        });

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(EmailLoginActivity.this, SignInActivity.class));
            }
        });
    }
}
