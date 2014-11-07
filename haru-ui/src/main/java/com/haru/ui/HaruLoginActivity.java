package com.haru.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.haru.HaruException;
import com.haru.User;
import com.haru.callback.LoginCallback;

/**
 *
 */
public class HaruLoginActivity extends FragmentActivity
        implements HaruLoginFragment.OnLoginFinishedListener,
        HaruLoginFragment.OnLostPasswordButtonListener,
        HaruLoginFragment.OnSignUpButtonListener {

    private FragmentManager mFragmentManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentManager = getSupportFragmentManager();
    }

    @Override
    public void onLoginFinished() {

    }

    @Override
    public void onLostPasswordButton() {
    }

    @Override
    public void onSignupButton() {

    }
}
