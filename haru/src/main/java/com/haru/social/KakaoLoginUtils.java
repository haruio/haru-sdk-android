package com.haru.social;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionState;
import com.haru.HaruException;
import com.haru.callback.LoginCallback;
import com.kakao.authorization.accesstoken.AccessToken;

import java.lang.ref.WeakReference;

/**
 * Kakao를 통해 Haru 회원가입 / 로그인을 할 수 있게 해주는 도구이다.
 */
public class KakaoLoginUtils {

    private static String appId;
    private static WeakReference<Activity> currentActivity;

    /**
     * Facebook SDK를 초기화한다.
     * @param context Application Context
     */
    public static void initialize(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);

            appId = appInfo.metaData.getString("com.facebook.sdk.ApplicationId");

        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Failed to find package information");
        }
    }


    /**
     *  Kakao SDK로 로그인 이후, 로그인 함수를 통해 받은 Access Token을 가지고 로그인한다.
     *  callback을 통해 User 객체가 반환되고, 에러 발생시 HaruException을 반환한다.
     *
     *  @param callback Login Callback
     */
    public static void logInAfterKakaoLogin(final LoginCallback callback) {
        if (com.kakao.Session.getCurrentSession() == null) {
            throw new IllegalAccessError("Kakao SDK를 통한 로그인 (Session.initializeSession) 이후 호출하세요.");
        }
        String accessToken = com.kakao.Session.getCurrentSession().getAccessToken();
        if (accessToken == null) {
            throw new RuntimeException("Kakao AccessToken을 가져오는데 실패했습니다. 로그인되어 있나요?");
        }
    }

    /**
     * {@link com.haru.social.KakaoLoginUtils#logIn(android.app.Activity, com.haru.callback.LoginCallback)}
     * Activity를 통해 로그인했을 경우에 해당 액티비티의 onActivityResult에서 호출해야 하는 함수이다.
     *
     * @param requestCode Activity에서 넘어온 requestCode
     * @param resultCode Activity에서 넘어온 resultCode
     * @param data Activity에서 넘어온 data (Intent)
     */
    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        Activity activity = currentActivity.get();
        if (activity != null) {
            Session.getActiveSession().onActivityResult(activity, requestCode, resultCode, data);
        }
    }
}

