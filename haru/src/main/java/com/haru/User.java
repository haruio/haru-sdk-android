package com.haru;

import android.util.Log;

import com.haru.callback.LoginCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

@ClassNameOfEntity(User.CLASS_NAME)
public class User extends Entity {

    // 서버에 저장되는 "엔티티 클래스" 이름
    public static final String CLASS_NAME = "Users";
    private static final String CURRENT_USER_TAG = "__currentUser";

    // 현재 유저
    private static User currentUser;

    // 이 유저의 세션 토큰
    private String sessionToken;

    // 패스워드. 서버에 저장되면 안되기에 따로 관리한다.
    private String password;

    public User() {
        super(CLASS_NAME);
    }

    /**
     * 현재 로그인된 User를 반환한다.
     * 만약, 로그인되어있으나 현재 유저가 로딩되어있지 않으면 로컬 데이터스토어로부터 로딩한다.
     *
     * @return 현재 로그인된 User 객체
     */
    public static User getCurrentUser() {

        // if already logined, just return the current user object
        if (currentUser != null) return currentUser;

        // not initialized?
        if (!LocalEntityStore.isInitialized()) {
            throw new RuntimeException("You need to call Haru.init() before using other APIs.");
        }

        // Now, try to retrieve user
        // get current user data
        ArrayList<Entity> entities =
                LocalEntityStore.retrieveEntitiesByTag(CLASS_NAME, CURRENT_USER_TAG);

        if (entities == null || entities.size() == 0) {
            // not logined. goodbye
            Log.i("Haru", "Not Logined!");
            return null;
        }

        // fill the current user data
        Log.i("Haru", "You logined");
        currentUser = (User) entities.get(0);
        currentUser.sessionToken = currentUser.getString("sessionToken");

        return currentUser;
    }

    /**
     * 현재 로그인된 User 세션의 토큰을 반환한다.
     *
     * @return sessionToken (미로그인시 null)
     */
    public static String getCurrentSessionToken() {
        User current = getCurrentUser();
        if (current != null) return current.sessionToken;
        else return null;
    }

    public static boolean isLogined() {
        return getCurrentUser() != null;
    }

    /**
     * 유저의 이메일을 설정한다.
     *
     * @param email 이메일
     */
    public void setEmail(String email) {
        put("email", email);
    }

    /**
     * 유저의 이메일을 반환한다.
     *
     * @return Email
     */
    public String getEmail() {
        return super.getString("email");
    }

    /**
     * 유저의 이름 (아이디)를 설정한다.
     *
     * @param name 유저 이름 (ID)
     */
    public void setUserName(String name) {
        super.put("username", name);
    }

    /**
     * 유저의 이름 (아이디)를 반환한다.
     *
     * @return 이름 (아이디)
     */
    public String getUserName() {
        return super.getString("username");
    }

    /**
     * 유저의 패스워드를 설정한다.
     * <p/>
     * 서버에 저장되지 않으며, 로컬에만 보존된다. 회원가입 시에만 써야 한다.
     *
     * @param password 패스워드
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 회원가입한다.
     * setUserName, setPassword를 통해서 가입 이전 사용자 정보가 작성되어 있어야 한다.
     * User#put을 이용해서 사용자 정의 데이터를 집어넣어도 회원가입시 같이 전달된다.
     *
     * @param callback 회원가입 이후 콜백
     * @return 회원가입 태스크
     */
    public Task signInInBackground(final LoginCallback callback) {

        final User me = User.this;

        // Write a register request to server.
        // Encode entire entity to JSON
        JSONObject param = (JSONObject) this.toJson();

        // password
        try {
            param.put("password", password);

        } catch (JSONException e) {
            e.printStackTrace();
            callback.done(null, new HaruException("JSONException: Wrong password type!", e));
            return null;
        }

        Task<HaruResponse> registerTask = Haru.newUserRequest("/users")
                .post(param)
                .executeAsync();

        return registerTask.onSuccessTask(new Continuation<HaruResponse, Task<Entity>>() {
            @Override
            public Task<Entity> then(Task<HaruResponse> task) throws Exception {

                HaruResponse response = task.getResult();
                if (response.hasError()) throw response.getError();

                // Register succeed. now we have to save current session
                String userId = response.getJsonBody().getString("_id");
                String sessionToken = response.getJsonBody().getString("sessionToken");

                me.sessionToken = sessionToken;
                me.put("sessionToken", sessionToken);

                // Retrieve me!
                setEntityId(userId);
                return me.fetchInBackground();

            }

        }, Task.BACKGROUND_EXECUTOR).continueWith(new Continuation<Entity, User>() {
            @Override
            public User then(Task<Entity> task) throws Exception {
                // Exception? just throw it
                if (task.isFaulted()) {
                    Exception e = task.getError();
                    e.printStackTrace();
                    callback.done(null, new HaruException(e));
                    throw e;
                }

                // Set current user to myself
                currentUser = me;

                Log.i("Haru", "I got a user => " + currentUser.get("email") + ", sessionToken=" + sessionToken);

                // Save current user
                LocalEntityStore.deleteTagFromLocal(CLASS_NAME, CURRENT_USER_TAG);
                LocalEntityStore.saveEntity(currentUser, CURRENT_USER_TAG);

                // Callback
                callback.done(currentUser, null);

                return currentUser;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * 로그인한다.
     *
     * @param userName 유저 ID
     * @param pw       패스워드
     * @param callback 로그인 이후 콜백
     * @return 로그인 태스크
     */
    public static Task logInInBackground(String userName, String pw, final LoginCallback callback) {
        // Write a login request to the user server.
        HaruRequest.Param param = new HaruRequest.Param();
        param.put("username", userName);
        param.put("password", pw);

        Task<HaruResponse> loginTask = Haru.newUserRequest("/login")
                .get(param)
                .executeAsync();

        return loginTask.onSuccessTask(new Continuation<HaruResponse, Task<Entity>>() {
            @Override
            public Task<Entity> then(Task<HaruResponse> task) throws Exception {

                HaruResponse response = task.getResult();
                if (response.hasError()) {
                    throw response.getError();
                }

                // Login succeed. now we have to save current session
                String userId = response.getJsonBody().getString("_id");
                String sessionToken = response.getJsonBody().getString("sessionToken");

                // Save some user data - before we fetch
                currentUser = new User();
                currentUser.entityId = userId;
                currentUser.sessionToken = sessionToken;

                // Retrieve user data - to know more about user.
                return User.retrieveTask(User.class, userId);

            }
        }, Task.BACKGROUND_EXECUTOR).continueWith(new Continuation<Entity, User>() {
            @Override
            public User then(Task<Entity> task) throws Exception {

                if (task.isFaulted()) {
                    // Exception? just log and throw it
                    Exception e = task.getError();
                    e.printStackTrace();
                    callback.done(null, new HaruException(e));
                    throw e;
                }

                // Fill the more information about user
                Entity fetchResult = task.getResult();
                currentUser.entityData = fetchResult.entityData;
                currentUser.createdAt = fetchResult.createdAt;
                currentUser.updatedAt = fetchResult.updatedAt;

                // Save current user
                LocalEntityStore.deleteTagFromLocal(CLASS_NAME, CURRENT_USER_TAG);
                LocalEntityStore.saveEntity(currentUser, CURRENT_USER_TAG);

                // Call LoginCallback
                callback.done(currentUser, null);

                // pass the result
                return currentUser;

            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    public static Task socialLogin(String socialProvider,
                            String userId,
                            String accessToken,
                            final LoginCallback callback) {

        // { deviceToken: 123, authData: { facebook: { ... }}} Format
        JSONObject request = new JSONObject();
        try {
            request.put("deviceToken",
                    Installation.getCurrentInstallation().getString("deviceToken"));

            JSONObject providerInfo = new JSONObject();
            providerInfo.put("id", userId);
            providerInfo.put("access_token", accessToken);

            JSONObject authData = new JSONObject();
            authData.put(socialProvider, providerInfo);
            request.put("authData", authData);

        } catch (JSONException e) {
            callback.done(null, new HaruException(e));
        }

        Task<HaruResponse> socialLoginTask = Haru.newUserRequest("/users")
                .post(request)
                .executeAsync();

        return socialLoginTask.onSuccessTask(new Continuation<HaruResponse, Task<Entity>>() {
            @Override
            public Task<Entity> then(Task<HaruResponse> task) throws Exception {

                HaruResponse response = task.getResult();
                if (response.hasError()) {
                    throw response.getError();
                }

                // Login succeed. now we have to save current session
                String userId = response.getJsonBody().getString("_id");
                String sessionToken = response.getJsonBody().getString("sessionToken");

                // Save some user data - before we fetch
                currentUser = new User();
                currentUser.entityId = userId;
                currentUser.sessionToken = sessionToken;

                // Retrieve user data - to know more about user.
                return User.retrieveTask(User.class, userId);

            }
        }, Task.BACKGROUND_EXECUTOR).continueWith(new Continuation<Entity, User>() {
            @Override
            public User then(Task<Entity> task) throws Exception {

                if (task.isFaulted()) {
                    // Exception? just log and throw it
                    Exception e = task.getError();
                    e.printStackTrace();
                    callback.done(null, new HaruException(e));
                    throw e;
                }

                // Fill the more information about user
                Entity fetchResult = task.getResult();
                currentUser.entityData = fetchResult.entityData;
                currentUser.createdAt = fetchResult.createdAt;
                currentUser.updatedAt = fetchResult.updatedAt;

                // Save current user
                LocalEntityStore.deleteTagFromLocal(CLASS_NAME, CURRENT_USER_TAG);
                LocalEntityStore.saveEntity(currentUser, CURRENT_USER_TAG);

                // Call LoginCallback
                callback.done(currentUser, null);

                // pass the result
                return currentUser;

            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * 현재 유저를 로그아웃시킨다.
     * @return 로그아웃 태스크 (서버로의)
     */
    public static Task logOutInBackground() {

        // Logout request to user server.
        Task<HaruResponse> logoutTask = Haru.newUserRequest("/logout/me").executeAsync();

        // Remove from local
        LocalEntityStore.deleteTagFromLocal(CLASS_NAME, CURRENT_USER_TAG);
        currentUser = null;

        return logoutTask;
    }
}
