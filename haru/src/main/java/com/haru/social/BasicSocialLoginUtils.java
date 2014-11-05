package com.haru.social;

import com.haru.Haru;
import com.haru.HaruException;
import com.haru.HaruResponse;
import com.haru.Installation;
import com.haru.User;
import com.haru.callback.LoginCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 소셜 로그인
 */
public abstract class BasicSocialLoginUtils {

    public static String getSocialProviderName() {
        return "Default";
    }

    public static void sendLogInRequest(String userId,
                                              String accessToken,
                                              LoginCallback callback) {

        // { deviceToken: 123, authData: { facebook: { ... }}} Format
        JSONObject request = new JSONObject();
        try {
            request.put("deviceToken",
                    Installation.getCurrentInstallation().getString("deviceToken"));

            JSONObject providerInfo = new JSONObject();
            providerInfo.put("id", userId);
            providerInfo.put("access_token", accessToken);

            JSONObject authData = new JSONObject();
            authData.put(getSocialProviderName(), providerInfo);
            request.put("authData", authData);

        } catch (JSONException e) {
            callback.done(null, new HaruException(e));
        }

        Task<HaruResponse> socialLoginTask = Haru.newUserRequest("/users")
                .post(request)
                .executeAsync();

        socialLoginTask.onSuccess(new Continuation<HaruResponse, User>() {
            @Override
            public User then(Task<HaruResponse> task) throws Exception {

                return null;
            }
        }).continueWith(new Continuation<User, User>() {
            @Override
            public User then(Task<User> task) throws Exception {
                if (task.isFaulted()) {
                    Haru.stackTrace(task.getError());
                    throw task.getError();
                }
                return task.getResult();
            }
        });
    }
}
