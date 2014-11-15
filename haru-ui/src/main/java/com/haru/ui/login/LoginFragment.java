package com.haru.ui.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
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

import com.haru.Haru;
import com.haru.HaruException;
import com.haru.User;
import com.haru.callback.LoginCallback;
import com.haru.social.FacebookLoginUtils;
import com.haru.social.KakaoLoginUtils;
import com.haru.ui.R;

/**
 *
 */
public class LoginFragment extends Fragment {

    public interface OnLoginFinishedListener {
        public void onLoginFinished(int activityResultCode);
    }

    public interface OnSignUpButtonListener {
        public void onSignupButton();
    }

    public interface OnLostPasswordButtonListener {
        public void onLostPasswordButton();
    }

    // Handlers
    private OnLoginFinishedListener mLoginFinishedListener;
    private OnSignUpButtonListener mSignUpButtonListener;
    private OnLostPasswordButtonListener mOnLostPasswordButtonListener;

    // UI references.
    private EditText mUserIdView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Button facebookLoginButton;
    private View kakaoLoginButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.haru_fragment_login, null, false);

        // Set up the login form.
        mUserIdView = (EditText) rootView.findViewById(R.id.user_id);

        mPasswordView = (EditText) rootView.findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button signInButton = (Button) rootView.findViewById(R.id.email_sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mProgressView = rootView.findViewById(R.id.login_progress);
        mLoginFormView = rootView.findViewById(R.id.email_login_form);

        // Facebook Social Login
        facebookLoginButton = (Button) rootView.findViewById(R.id.social_facebook_login_button);
        facebookLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Log in using FacebookLoginUtils.
                FacebookLoginUtils.logIn(getActivity(), new LoginCallback() {
                    @Override
                    public void done(User user, HaruException error) {
                        // call OnLoginFinished callback
                        if (mLoginFinishedListener != null) {
                            mLoginFinishedListener.onLoginFinished(Activity.RESULT_OK);
                        }

                        // that's all! done.
                    }
                });
            }
        });

        // Kakao Login
        kakaoLoginButton = rootView.findViewById(R.id.haru_social_kakao_login_button);
        kakaoLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Log in using KakaoLoginUtils.
                KakaoLoginUtils.logIn(getActivity(), new LoginCallback() {
                    @Override
                    public void done(User user, HaruException error) {
                        // call OnLoginFinished callback
                        if (mLoginFinishedListener != null) {
                            mLoginFinishedListener.onLoginFinished(Activity.RESULT_OK);
                        }
                    }
                });
            }
        });

        // Signup button
        Button signUpButton = (Button) rootView.findViewById(R.id.email_sign_up_button);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSignUpButtonListener != null) {
                    mSignUpButtonListener.onSignupButton();
                }
            }
        });

        // Lost password button
        Button forgotPasswordButton = (Button) rootView.findViewById(R.id.forgot_password_button);
        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnLostPasswordButtonListener != null) {
                    mOnLostPasswordButtonListener.onLostPasswordButton();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnLoginFinishedListener) {
            mLoginFinishedListener = (OnLoginFinishedListener) activity;
        }

        if (activity instanceof OnSignUpButtonListener) {
            mSignUpButtonListener = (OnSignUpButtonListener) activity;
        }

        if (activity instanceof OnLostPasswordButtonListener) {
            mOnLostPasswordButtonListener = (OnLostPasswordButtonListener) activity;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Haru.logD("onActivityResult Facebook");
        FacebookLoginUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        KakaoLoginUtils.onResume(this);
    }

    void attemptLogin() {

        // Reset errors.
        mUserIdView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String userId = mUserIdView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.haru_error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(userId)) {
            mUserIdView.setError(getString(R.string.haru_error_field_required));
            focusView = mUserIdView;
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
            doLogin(userId, password);
        }
    }

    void doLogin(String userId, String password) {
        // Log in into haru
        User.logInInBackground(userId, password, new LoginCallback() {
            @Override
            public void done(User user, HaruException error) {

                // disable progress
                showProgress(false);

                // check if there's error
                if (error != null) {
                    // Username is missing?
                    if (error.getErrorCode() == HaruException.NO_SUCH_ACCOUNT) {
                        mPasswordView.setError("Username or password is wrong!");
                        mPasswordView.requestFocus();
                    }
                    Toast.makeText(getActivity(), "Problem occured", Toast.LENGTH_SHORT).show();
                    return;
                }

                // call OnLoginFinished callback
                if (mLoginFinishedListener != null) {
                    mLoginFinishedListener.onLoginFinished(Activity.RESULT_OK);
                }

                // that's all! done.
            }
        });
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
