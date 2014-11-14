package com.haru.ui.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.haru.HaruException;
import com.haru.User;
import com.haru.callback.LoginCallback;
import com.haru.ui.R;

/**
 *
 */
public class SignUpFragment extends Fragment {

    // UI references.
    private EditText mUserIdView, mEmailView;
    private EditText mPasswordView, mPasswordRepeatView;
    private View mProgressView;
    private View mLoginFormView;

    public static interface OnSignUpFinishedListener {
        public void onSignUpFinished(int activityResultCode);
    }

    private OnSignUpFinishedListener mSignUpFinishedListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.haru_fragment_sign_up, null, false);

        // Set up the login form.
        mUserIdView = (EditText) rootView.findViewById(R.id.user_id);
        mEmailView = (EditText) rootView.findViewById(R.id.email);

        mPasswordView = (EditText) rootView.findViewById(R.id.password);
        mPasswordRepeatView = (EditText) rootView.findViewById(R.id.password_repeat);
        mPasswordRepeatView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.signUp || id == EditorInfo.IME_NULL) {
                    attemptSignUp();
                    return true;
                }
                return false;
            }
        });

        Button signUpButton = (Button) rootView.findViewById(R.id.sign_up_button);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignUp();
            }
        });

        mProgressView = rootView.findViewById(R.id.signup_progress);
        mLoginFormView = rootView.findViewById(R.id.email_login_form);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnSignUpFinishedListener) {
            mSignUpFinishedListener = (OnSignUpFinishedListener) activity;
        }
    }

    void attemptSignUp() {

        // Reset errors.
        mUserIdView.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mPasswordRepeatView.setError(null);

        // Store values at the time of the login attempt.
        String userId = mUserIdView.getText().toString();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String passwordRepeat = mPasswordRepeatView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.haru_error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a password repeat field.
        if (TextUtils.isEmpty(passwordRepeat)) {
            mPasswordRepeatView.setError(getString(R.string.haru_error_field_required));
            focusView = mPasswordRepeatView;
            cancel = true;
        }

        // password is matching?
        if (!password.equals(passwordRepeat)) {
            mPasswordRepeatView.setError(getString(R.string.haru_error_incorrect_password));
            focusView = mPasswordRepeatView;
            cancel = true;
        }

        // Check for a valid id.
        if (TextUtils.isEmpty(userId)) {
            mUserIdView.setError(getString(R.string.haru_error_field_required));
            focusView = mUserIdView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email) || !isEmailValid(email)) {
            mEmailView.setError(getString(R.string.haru_error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }


        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            // Sign up into haru
            doSignUp(userId, email, password);
        }
    }

    private void doSignUp(String userId, String email, String password) {
        User user = new User();
        user.setUserName(userId);
        user.setEmail(email);
        user.setPassword(password);

        user.signUpInBackground(new LoginCallback() {
            @Override
            public void done(User user, HaruException error) {

                // disable progress
                showProgress(false);

                // check if there's error
                if (error != null) {
                    Toast.makeText(getActivity(), "Problem occured", Toast.LENGTH_SHORT).show();
                    return;
                }

                // call OnLoginFinished callback
                if (mSignUpFinishedListener != null) {
                    mSignUpFinishedListener.onSignUpFinished(Activity.RESULT_OK);
                }

                // that's all! done.
            }
        });
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }


    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
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
