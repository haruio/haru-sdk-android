package com.haru.ui.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.haru.HaruException;
import com.haru.User;
import com.haru.callback.LoginCallback;
import com.haru.ui.R;

/**
 *
 */
public class LostPasswordFragment extends Fragment {

    public interface OnFindPasswordSubmitListener {
        public void onFindPasswordSubmit(int activityResultCode);
    }

    // Handlers
    private OnFindPasswordSubmitListener mFindPasswordSubmitListener;

    // UI references.
    private EditText mEmailView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.haru_fragment_forgot_password, null, false);

        // Set up the login form.
        mEmailView = (EditText) rootView.findViewById(R.id.email);

        Button mSubmitButton = (Button) rootView.findViewById(R.id.email_sign_in_button);
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSubmit();
            }
        });

        mProgressView = rootView.findViewById(R.id.submit_progress);
        mLoginFormView = rootView.findViewById(R.id.email_login_form);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnFindPasswordSubmitListener) {
            mFindPasswordSubmitListener = (OnFindPasswordSubmitListener) activity;
        }
    }

    void attemptSubmit() {

        // Reset errors.
        mEmailView.setError(null);

        // Store values at the time of the submit attempt.
        String email = mEmailView.getText().toString();

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(email) && !isEmailValid(email)) {
            mEmailView.setError(getString(R.string.haru_error_invalid_email));
            mEmailView.requestFocus();

        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the password finding request submit attempt.
            showProgress(true);
            doSubmit(email);
        }
    }

    void doSubmit(String email) {
        // Log in into haru
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
