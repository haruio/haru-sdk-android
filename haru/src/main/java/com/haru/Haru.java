package com.haru;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.*;

/**
 * Haru Android SDK를 초기화하는 역할이자,
 * 각종 함수들을 모아놓은 Utility Class의 역할을 한다.
 */
public class Haru {

    private static final String SDK_VERSION_NAME = "0.1.0-alpha";
    private static final boolean IS_DEBUG_BUILD = true;

    private static final String TAG = "Haru";

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

        mAppKey = appKey;
        mSdkKey = sdkKey;

        // Entity에 등록
        Entity.registerSubclass(Installation.class);
        Entity.registerSubclass(User.class);

        HaruRequest.initialize(context);
        useOfflineDataStoring(context);
        Installation.init(context);
        Config.init(context);
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
                Haru.logD("JSON %s : Double?", key);
                value = json.getDouble(key);

            } catch (Exception e) {

                try {
                    Haru.logD("JSON %s : Long?", key);
                    value = json.getLong(key);

                } catch (JSONException le) {
                    try {
                        Haru.logD("JSON %s : Integer?", key);
                        value = json.getInt(key);

                    } catch (JSONException ie) {
                        // it's probably a boolean?
                        try {
                            Haru.logD("JSON %s : Bool?", key);
                            value = json.getBoolean(key);

                        } catch (JSONException je) {
                            // or JSONArray?
                            try {
                                // TODO: Nested array 처리
                                Haru.logD("JSON %s : Array?", key);
                                value = json.getJSONArray(key);

                            } catch (JSONException jee) {
                                // or JSONObject?
                                try {
                                    Haru.logD("JSON %s : JSON?", key);
                                    JSONObject nestedJson = json.getJSONObject(key);
                                    Map<String, Object> nestedMap = new HashMap<String, Object>();
                                    convertJsonToMap(nestedJson, nestedMap);
                                    value = nestedMap;

                                } catch (JSONException jeee) {
                                    // okay, it's just a string...
                                    try {
                                        Haru.logD("JSON %s : String!", key);
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
            Haru.logD(e.getMessage());
            StackTraceElement[] traces = e.getStackTrace();
            for (StackTraceElement elem : traces) {
                Haru.logD(elem.toString());
            }
        }
    }

    public static void logD(String message, Object ...args) {
        if (IS_DEBUG_BUILD) Log.d(TAG, String.format(message, args));
    }

    public static void logI(String message, Object ...args) {
        Log.i(TAG, String.format(message, args));
    }


}
