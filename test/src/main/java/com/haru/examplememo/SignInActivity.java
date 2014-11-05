package com.haru.examplememo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.haru.HaruException;
import com.haru.User;
import com.haru.callback.LoginCallback;

/**
 * 샘플 메모앱 using Haru
 */
public class SignInActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in);

        final EditText usernameInput = (EditText) findViewById(R.id.usernameInput),
                emailInput = (EditText) findViewById(R.id.emailInput),
                pwInput = (EditText) findViewById(R.id.passwordInput),
                pwAgainInput = (EditText) findViewById(R.id.passwordAgainInput);

        Button signInButton = (Button) findViewById(R.id.signInButton);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (usernameInput.getText().length() < 4 ||
                        emailInput.getText().length() < 4 ||
                        pwInput.getText().length() == 0 ||
                        pwAgainInput.getText().length() == 0) {

                    showToast("필수 항목들을 전부 작성해 주세요.");
                    return;
                }

                if (!pwInput.getText().toString().equals(pwAgainInput.getText().toString())) {
                    showToast("비밀번호와 비밀번호 확인이 일치해야 합니다.");
                    return;
                }

                // 서버에 유저 저장
                User user = new User();
                user.setUserName(usernameInput.getText().toString());
                user.setEmail(emailInput.getText().toString());
                user.setPassword(pwInput.getText().toString());

                user.signInInBackground(new LoginCallback() {
                    @Override
                    public void done(User user, HaruException error) {
                        if (error != null) {
                            error.printStackTrace();
                            showToast(error.getMessage());
                            Log.e("Haru", error.getStackTrace().toString());
                            return;
                        }

                        showToast("회원가입 완료 : " + user.getId() + ", 이름=" + user.getUserName());
                        startActivity(new Intent(SignInActivity.this, MainActivity.class));
                        finish();
                    }
                });
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
