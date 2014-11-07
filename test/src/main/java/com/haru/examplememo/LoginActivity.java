package com.haru.examplememo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
public class LoginActivity extends Activity {

    private MySessionStatusCallback kakaoCallback;

    private View loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 로그인되어있으면 바로 메인으로
        if (User.isLogined()) {
            startActivity(new Intent(this, MainActivity.class));
            return;
        }

        setContentView(R.layout.login_activity);

        loginButton = findViewById(R.id.kakaoLogin);

        findViewById(R.id.facebookLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Session.getCurrentSession() == null
                        || !Session.getCurrentSession().isOpened()) {
                    facebookLogin();

                } else {
                    Session.getCurrentSession().close(new SessionCallback() {
                        @Override
                        public void onSessionOpened() {

                        }

                        @Override
                        public void onSessionClosed(KakaoException exception) {

                        }
                    });
                }
            }
        });

        findViewById(R.id.emailLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, EmailLoginActivity.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // FacebookLoginUtils로 결과값을 전달
        FacebookLoginUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 세션을 초기화 한다. 카카오톡으로만 로그인을 유도하고 싶다면 Session.initializeSession(this, mySessionCallback, AuthType.KAKAO_TALK)
        if(Session.initializeSession(this, kakaoCallback)){
            // 1. 세션을 갱신 중이면, 프로그레스바를 보이거나 버튼을 숨기는 등의 액션을 취한다
            loginButton.setVisibility(View.GONE);

        } else if (Session.getCurrentSession().isOpened()){
            onSessionOpened();
        }
        // 3. else 로그인 창이 보인다.
    }

    private class MySessionStatusCallback implements SessionCallback {
        @Override
        public void onSessionOpened() {
            // 프로그레스바를 보이고 있었다면 중지하고 세션 오픈후 보일 페이지로 이동
            LoginActivity.this.onSessionOpened();
        }

        @Override
        public void onSessionClosed(final KakaoException exception) {
            // 프로그레스바를 보이고 있었다면 중지하고 세션 오픈을 못했으니 다시 로그인 버튼 노출.
            loginButton.setVisibility(View.VISIBLE);
        }

    }

    private void onSessionOpened() {
        KakaoLoginUtils.logInAfterKakaoLogined(new LoginCallback() {
            @Override
            public void done(User user, HaruException error) {
            }
        });
    }

    void facebookLogin() {
        FacebookLoginUtils.logIn(LoginActivity.this, new LoginCallback() {
            @Override
            public void done(User user, HaruException error) {
                if (error != null) {
                    error.printStackTrace();
                    Toast.makeText(LoginActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
