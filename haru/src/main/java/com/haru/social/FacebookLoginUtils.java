package com.haru.social;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.android.Util;
import com.facebook.internal.Utility;
import com.facebook.model.GraphUser;
import com.haru.Haru;
import com.haru.HaruException;
import com.haru.HaruResponse;
import com.haru.Installation;
import com.haru.User;
import com.haru.callback.LoginCallback;
import com.haru.task.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Facebook을 통해 Haru 회원가입 / 로그인을 할 수 있게 해주는 도구이다.
 */
public class FacebookLoginUtils {

    private static WeakReference<Activity> currentActivity;

    /**
     *  Facebook SDK의 함수를 직접 호출해줄 필요 없이, 자동으로 로그인해준다.
     *  callback을 통해 User 객체가 반환되고, 에러 발생시 HaruException을 반환한다.
     *
     *  @param activity Facebook Login을 호출하는 activity
     *  @param callback Login Callback
     */
    public static void logIn(Activity activity, final LoginCallback callback) {
        currentActivity = new WeakReference<Activity>(activity);

        Session.openActiveSession(activity, true, new Session.StatusCallback() {
            @Override
            public void call(final Session session, SessionState state, Exception exception) {
                // failed to log in into facebook?
                if (exception != null) {
                    Haru.stackTrace(exception);
                    callback.done(null, new HaruException(exception));
                    return;
                }

                // wait until session to open
                if (session.isOpened()) {
                    // Facebook login succeed. now we need to retrieve user information
                    Request.newMeRequest(session, new Request.GraphUserCallback() {
                        @Override
                        public void onCompleted(GraphUser user, Response response) {

                            Haru.logD("Facebook user info : %s", user.getId());

                            // Log in to Haru server
                            User.socialLogin("facebook",
                                    user.getId(),
                                    session.getAccessToken(),
                                    callback);
                        }
                    }).executeAsync();

                    Haru.logD("facebookLogin finished => %s", session.getAccessToken());
                }
            }
        });
    }

    /**
     *  Facebook SDK의 로그인 함수를 호출한 뒤에, 세션이 열린 상태에서 로그인한다.
     *  callback을 통해 User 객체가 반환되고, 에러 발생시 HaruException을 반환한다.
     *
     *  @param context Application Context
     *  @param callback Login Callback
     */
    public static void logInAfterFacebookLogined(Context context,
                             final LoginCallback callback) {

        Session activeSession = Session.getActiveSession();
        if (activeSession != null && activeSession.isOpened()) {
            // we need to retrieve user information
            Request.newMeRequest(activeSession, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user, Response response) {

                    // Log in to Haru server
                    User.socialLogin("facebook",
                            user.getId(),
                            Session.getActiveSession().getAccessToken(),
                            callback);
                }
            });
        } else {
            callback.done(null, new HaruException(
                    "Facebook session is not opened!" +
                    "you need to login using Facebook SDK before calling this."));
        }
    }

    /**
     * {@link FacebookLoginUtils#logIn(Activity, LoginCallback)}
     * Activity를 통해 로그인했을 경우에 해당 액티비티의 onActivityResult에서 호출해야 하는 함수이다.
     *
     * @param requestCode Activity에서 넘어온 requestCode
     * @param resultCode Activity에서 넘어온 resultCode
     * @param data Activity에서 넘어온 data (Intent)
     */
    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("Haru", "onActivityResult");
        Activity activity = currentActivity.get();
        if (activity != null) {
            Session.getActiveSession().onActivityResult(activity, requestCode, resultCode, data);
        }
    }

    public static void logout() {
        Context context = Haru.getAppContext();

        // clear caches
        Utility.clearCaches(context);
        Utility.clearFacebookCookies(context);

        // clear Facebook token store
        SharedPreferences.Editor tokenStrategy = context.getSharedPreferences(
                "com.facebook.SharedPreferencesTokenCachingStrategy.DEFAULT_KEY",
                Context.MODE_PRIVATE).edit();

        tokenStrategy.clear();
        tokenStrategy.apply();
    }
}


