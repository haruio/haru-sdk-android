package com.haru;

import com.haru.callback.LoginCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;

public class User extends Entity {

    private static User currentUser;

    private String sessionToken;
    private String email, password, userName;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLogined() {
        return currentUser == null;
    }

    public static Task logInInBackground(String username, String pw, LoginCallback callback) {
        HaruRequest.Param param = new HaruRequest.Param();
        param.put("username", username);
        param.put("password", pw);

        Task<HaruResponse> loginTask = Haru.newUserRequest("/login")
                .get(param)
                .executeAsync();

        return loginTask.onSuccess(new Continuation<HaruResponse, User>() {
            @Override
            public User then(Task<HaruResponse> task) throws Exception {
                return null;
            }
        }).continueWith(new Continuation<User, User>() {
            @Override
            public User then(Task<User> task) throws Exception {
                if (task.isFaulted()) {

                }
                User user = task.getResult();
                return user;
            }
        });
    }

    public static Task logOutInBackground() {
        return null;
    }
}
