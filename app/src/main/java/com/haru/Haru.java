package com.haru;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public class Haru {

    private static final String AUTH_SERVER = "http://stage.haru.io:3000";

    // API 서버, Write 서버의 주소는 인증서버에서 가져온다. 기본값은 이거.
    private static String apiServer = "http://stage.haru.io:3000/1";
    private static String writeServer = "http://stage.haru.io:8000/1";

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
    public static void initialize(Context context, String appKey, String sdkKey) {
        if (context == null) {
            throw new IllegalArgumentException("A context must be given.");
        }
        context = context.getApplicationContext();

        mAppKey = appKey;
        mSdkKey = sdkKey;

        HaruRequest.initialize(context);
    }

    /**
     * Haru API (Read) Server에 요청을 하기 위한 HaruRequest를 generate한다.
     * @param url Relative URL (ex: /classes/Foo)
     * @return HaruRequest
     */
    public static HaruRequest newApiRequest(String url) {
        return new HaruRequest(urlJoin(apiServer, url));
    }

    /**
     * Haru 인증 서버에 요청을 하기 위한 HaruRequest를 generate한다.
     * @param url Relative URL (ex: /classes/Foo)
     * @return HaruRequest
     */
    public static HaruRequest newAuthRequest(String url) {
        return new HaruRequest(urlJoin(AUTH_SERVER, url));
    }

    /**
     * Haru 쓰기 (Write) Server에 요청을 하기 위한 HaruRequest를 generate한다.
     * @param url Relative URL (ex: /classes/Foo)
     * @return HaruRequest
     */
    public static HaruRequest newWriteRequest(String url) {
        return new HaruRequest(urlJoin(writeServer, url));
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
            if (object instanceof Encodable) {
                return ((Encodable)object).encode();

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

    /**
     * JSON Object를 Map형태로 디코딩한다.
     * @param json JSON Object
     * @return HashMap
     */
    public static Map<String, Object> convertJsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<String, Object>();
        convertJsonToMap(json, map);
        return map;
    }

    private static void convertJsonToMap(JSONObject json, Map<String, Object> outputMap) {
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = null;

            // Try to parse value
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

                            } catch (JSONException jeeee) {

                                try {
                                    value = json.get(key);

                                } catch (JSONException jeeeee) {
                                    throw new RuntimeException("json parse failed");
                                }
                            }
                        }
                    }
                }
            }
            outputMap.put(key, value);
        }
    }

    /**
     * URL들을 적정 포맷으로 합친다.
     * @param urlpaths URL들
     * @return 합쳐진 완성 URL
     */
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
}
