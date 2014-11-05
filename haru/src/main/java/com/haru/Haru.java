package com.haru;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.*;

public class Haru {

    private static final String SDK_VERSION_NAME = "0.1.0-alpha";
    private static final boolean IS_DEBUG_BUILD = true;

    private static final String TAG = "Haru";

    private static final String AUTH_SERVER = "http://stage.haru.io:10000";

    // API 서버, Write 서버의 주소는 인증서버에서 가져온다. 기본값은 이거.
    private static String apiServer = "http://stage.haru.io:10100/1";
    private static String writeServer = "http://stage.haru.io:10200/1";
    private static String mqttPushServer = "http://stage.haru.io:10300";
    private static String userServer = "http://stage.haru.io:10400/1";
    private static String fileServer = "http://stage.haru.io:10500/";
    private static String helpCenterServer = "http://stage.haru.io:3000/";

    private static Context appContext;

    private static String mAppKey;
    private static String mSdkKey;

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
        appContext = context;

        mAppKey = appKey;
        mSdkKey = sdkKey;

        // Entity에 등록
        Entity.registerSubclass(Installation.class);
        Entity.registerSubclass(User.class);

        HaruRequest.initialize(context);
        useOfflineDataStoring(context);
        Installation.init(context);

        Log.e("Haru", "Device Token ==> " + Installation.getCurrentInstallation().getString("deviceToken"));

        // 서버 주소를 받아온다.
        try {
/*            Task authTask = newAuthRequest("/account")
                    .post(new HaruRequest.Param())
                    .executeAsync();

            authTask.waitForCompletion();

            if (authTask.isFaulted()) {
                throw authTask.getError();
            }
            HaruResponse result = (HaruResponse) authTask.getResult();
            if (result.hasError()) throw result.getError();

            JSONObject body = result.getJsonBody();
            apiServer = body.getJSONObject("readServer").getString("host");
            writeServer = body.getJSONObject("writeServer").getString("host");
            mqttPushServer = body.getJSONObject("pushServer").getJSONObject("mqtt").getString("host");

            Log.d("Haru", "API Server : " + apiServer);
            Log.d("Haru", "Write Server : " + writeServer);
            Log.d("Haru", "Push Server : " + mqttPushServer);*/

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void useOfflineDataStoring(Context context) {
        LocalEntityStore.initialize(context);
    }

    public static String getSdkVersion() {
        return SDK_VERSION_NAME;
    }

    public static HaruRequest newApiRequest(String url) {
        return new HaruRequest(urlJoin(apiServer, url));
    }

    public static HaruRequest newAuthRequest(String url) {
        return new HaruRequest(urlJoin(AUTH_SERVER, url));
    }

    public static HaruRequest newWriteRequest(String url) {
        return new HaruRequest(urlJoin(writeServer, url));
    }

    public static HaruRequest newPushRequest(String url) {
        return new HaruRequest(urlJoin(mqttPushServer, url));
    }

    public static HaruRequest newUserRequest(String url) {
        return new HaruRequest(urlJoin(userServer, url));
    }

    public static HaruRequest newFileRequest(String url) {
        return new HaruRequest(urlJoin(fileServer, url));
    }

    public static HaruRequest helpCenterRequest(String url) {
        return new HaruRequest(urlJoin(helpCenterServer, url));
    }


    private static boolean isEncodable(Object o) {
        return (o instanceof String) || (o instanceof Boolean) || (o instanceof Number) || (o instanceof Date);
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

            } else if (isEncodable(object)) {
                return String.valueOf(object);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        throw new IllegalArgumentException("The given object " + object.toString() + " is not encodable.");
    }

    private static void convertJsonToMap(JSONObject json, Map<String, Object> outputMap) {
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = null;

            // Try to parse name
            try {
                value = NumberFormat.getInstance().parse(String.valueOf(json.get(key)));

            } catch (Exception e) {
                // it's probably a boolean?
                try {
                    value = json.getBoolean(key);

                } catch (JSONException je) {
                    // or JSONArray?
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
                            // okay, it's just a string...
                            try {
                                value = json.getString(key);
                            } catch (JSONException j) {
                                throw new RuntimeException("Unknown type!");
                            }
                        }
                    }
                }
            }
            outputMap.put(key, value);
        }
    }

    public static Map<String, Object> convertJsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<String, Object>();
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
            StackTraceElement[] traces = e.getStackTrace();
            for (StackTraceElement elem : traces) {
                Haru.logD(elem.toString());
            }
        }
    }

    public static void logD(String message, Object ...args) {
        Log.d(TAG, String.format(message, args));
    }


}
