package com.haru;

import android.content.Context;
import android.util.Log;

import com.haru.callback.SaveCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Haru Android SDK를 초기화하는 역할이자,
 * 각종 함수들을 모아놓은 Utility Class의 역할을 한다.
 */
public class Haru {

    private static final String SDK_VERSION_NAME = "0.1.0-alpha";
    private static final boolean IS_DEBUG_BUILD = true;

    private static final String TAG = "Plugy";

    private static String mAppKey;
    private static String mSdkKey;
    private static Context mContext;

    /**
     * Haru Android SDK를 초기화한다.
     * 앱이 초기화될 시점에 호출되어야만 한다.
     *
     * @param context Application Context
     * @param appKey Application Key - Haru 대시보드 > 설정에서 확인
     * @param sdkKey SDK Key - Haru 대시보드 > 설정에서 확인
     */
    public static void init(Context context, String appKey, String sdkKey) {
        if (context == null) {
            throw new IllegalArgumentException("A context must be given.");
        }
        context = context.getApplicationContext();

        mAppKey = appKey;
        mSdkKey = sdkKey;
        mContext = context;

        // Entity에 등록
        Entity.registerSubclass(Installation.class);
        Entity.registerSubclass(User.class);

        HaruRequest.initialize(context);
        useOfflineDataStoring(context);
        Config.init(context);
    }

    public static Context getAppContext() {
        return mContext;
    }

    private static void useOfflineDataStoring(Context context) {
        LocalEntityStore.initialize(context);
    }

    public static String getSdkVersion() {
        return SDK_VERSION_NAME;
    }

    private static boolean isPrimitiveType(Object o) {
        return (o instanceof String)
                || (o instanceof Boolean)
                || (o instanceof Number)
                || (o instanceof Date);
    }

    /**
     * Encodable 객체들을 JSON 포맷으로 인코딩한다.
     * @param object 인코딩할 객체 (Encodable, Array)
     * @return 인코딩된 JSON (JSONObject or JSONArray)
     */
    public static Object encode(Object object) {
        try {
            if (object instanceof JsonEncodable) {
                return ((JsonEncodable)object).toJson();

            } else if (object instanceof List) {
                JSONArray jsonArray = new JSONArray();

                Iterator iterator = ((List) object).iterator();
                while (iterator.hasNext()) {
                    jsonArray.put(Haru.encode(iterator.next()));
                }
                return jsonArray;

            } else if (object instanceof JSONArray || object instanceof JSONObject) {
                return object;

            } else if (object instanceof Map) {
                return new JSONObject((Map)object);

            } else if (isPrimitiveType(object)) {
                return String.valueOf(object);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        throw new IllegalArgumentException("The given object " + object.toString() + " is not encodable.");
    }

    private static void convertJsonToMap(JSONObject json, Map<String, Object> outputMap) {
        Iterator keys = json.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = null;

            // I know it's very dirty way, but it's the only way
            // to convert JSON values to actual types.
            // Try to parse name
            try {
                value = json.getInt(key);

            } catch (Exception e) {
                try {
                    value = json.getLong(key);

                } catch (JSONException le) {
                    try {
                        value = json.getDouble(key);

                    } catch (JSONException ie) {
                        try {
                            value = json.getBoolean(key);

                        } catch (JSONException je) {
                            try {
                                // TODO: Nested array 처리
                                value = json.getJSONArray(key);

                            } catch (JSONException jee) {
                                // or JSONObject?
                                try {
                                    JSONObject nestedJson = json.getJSONObject(key);
                                    Map<String, Object> nestedMap = new HashMap<String, Object>();
                                    convertJsonToMap(nestedJson, nestedMap);
                                    value = nestedMap;

                                } catch (JSONException jeee) {
                                    try {
                                        value = json.getString(key);

                                    } catch (JSONException j) {
                                        throw new RuntimeException("Unknown type!");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            outputMap.put(key, value);
        }
    }

    public static HashMap<String, Object> convertJsonToMap(JSONObject json) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        convertJsonToMap(json, map);
        return map;
    }

    public static String urlJoin(String ...urlpaths) {
        StringBuilder builder = new StringBuilder();

        for (int i=0; i<urlpaths.length; i++) {
            String path = urlpaths[i];
            if (i != 0 && path.startsWith("/")) path = path.substring(1);
            else if (i != urlpaths.length - 1 && !path.endsWith("/")) path += "/";

            builder.append(path);
        }
        return builder.toString();
    }

    public static String getAppKey() {
        return mAppKey;
    }

    public static String getSdkKey() {
        return mSdkKey;
    }


    public static void stackTrace(Exception e) {
        if (IS_DEBUG_BUILD) {
            Log.e(TAG, e.getMessage());
            StackTraceElement[] traces = e.getStackTrace();
            for (StackTraceElement elem : traces) {
                Log.e(TAG, "    " + elem.toString());
            }
        }
    }

    public static void logD(String message, Object ...args) {
        if (IS_DEBUG_BUILD) Log.d(TAG, String.format(message, args));
    }

    public static void logE(String message, Object ...args) {
        Log.e(TAG, String.format(message, args));
    }

    public static void logI(String message, Object ...args) {
        Log.i(TAG, String.format(message, args));
    }


    public static Task trackPurchase(String product, String currency, int price) {
        return trackPurchase(product, currency, price, null);
    }

    /**
     * 인앱결제시 수익내역을 기록한다.
     * haru.io 관리자 페이지의 Monetization 메뉴에서 확인할 수 있다.
     *
     * @param product 구매재품 (null일 시 현재 유저의 이메일 사용)
     * @param currency 어느나라 통화
     * @param price 가격
     * @param callback 콜백
     * @return Task
     */
    public static Task trackPurchase(String product,
                                     String currency,
                                     int price,
                                    final SaveCallback callback) {
        Param param = new Param();
        param.put("productName", product);
        param.put("currencyCode", currency);
        param.put("price", price);

        User currentuser = User.getCurrentUser();
        if(currentuser != null){
            param.put("userId", currentuser.getId());
        } else {
            param.put("userId", "");
        }
        Installation currentinstallation = Installation.getCurrentInstallation();
        if(currentinstallation != null){
            param.put("national", currentinstallation.get("nation"));
            param.put("appVersion", currentinstallation.get("appVersion"));
            param.put("androidVersion", currentinstallation.get("androidVersion"));
            param.put("device", currentinstallation.get("device"));
        } else {
            param.put("national", "");
            param.put("appVersion", "");
            param.put("androidVersion", "");
            param.put("device", "");
        }


        // request to help center server
        Task<HaruResponse> trackPurchaseTask = new HaruRequest("/monetization")
                .post(param)
                .executeAsync();

        return trackPurchaseTask.continueWith(new Continuation<HaruResponse, HaruResponse>() {
            @Override
            public HaruResponse then(Task<HaruResponse> task) throws Exception {

                // error handling
                if (task.isFaulted()) {
                    Haru.stackTrace(task.getError());
                    if (callback != null) callback.done(new HaruException(task.getError()));
                    throw task.getError();

                } else if (task.getResult().hasError()) {
                    Exception e = task.getResult().getError();
                    Haru.stackTrace(e);
                    if (callback != null) callback.done(new HaruException(e));
                    throw e;
                }

                // callback
                if (callback != null) callback.done(null);

                return task.getResult();
            }
        });
    }
}
