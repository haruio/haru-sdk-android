package com.haru.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.WindowManager;

import com.haru.Haru;
import com.haru.social.FacebookLoginUtils;
import com.haru.ui.R;

/**
 * Haru's Default login screen template.
 */
public class LoginActivity extends FragmentActivity
        implements LoginFragment.OnLoginFinishedListener,
        LoginFragment.OnLostPasswordButtonListener,
        LoginFragment.OnSignUpButtonListener,
        SignUpFragment.OnSignUpFinishedListener,
        LostPasswordFragment.OnFindPasswordSubmitListener {

    public static final int REQUEST_CODE = 1001;

    private FragmentManager mFragmentManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.haru_activity_fragment);

        // Set activity default style
        setTheme(R.style.Haru_Theme_Login);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Add default login fragment
        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.beginTransaction()
                .add(R.id.haru_container, new LoginFragment())
                .commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Haru.logD("onActivityResult Facebook => Activity");
        FacebookLoginUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onLoginFinished(int activityResultCode) {
        setResult(activityResultCode);
        finish();
    }

    @Override
    public void onSignUpFinished(int activityResultCode) {
        setResult(activityResultCode);
        finish();
    }

    @Override
    public void onFindPasswordSubmit(int activityResultCode) {
        setResult(activityResultCode);
        finish();
    }

    @Override
    public void onLostPasswordButton() {
        // Go to Lost password screen
        mFragmentManager.beginTransaction()
                .replace(R.id.haru_container, new LostPasswordFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onSignupButton() {
        // Go to Signup screen
        mFragmentManager.beginTransaction()
                .replace(R.id.haru_container, new SignUpFragment())
                .addToBackStack(null)
                .commit();
    }
}
