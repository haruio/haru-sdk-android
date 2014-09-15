package com.haru;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

/**
 * Created by vista on 14. 9. 2..
 */
public class Haru {

    private static final String API_SERVER = "http://stage.haru.io";
    private static final String AUTH_SERVER = "http://stage.haru.io:3000";
    private static final String WRITE_SERVER = "http://stage.haru.io:8000";

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

    public static HaruRequest newApiRequest(String url) {
        return new HaruRequest(API_SERVER + url);
    }

    public static HaruRequest newAuthRequest(String url) {
        return new HaruRequest(AUTH_SERVER + url);
    }

    public static HaruRequest newWriteRequest(String url) {
        return new HaruRequest(WRITE_SERVER + url);
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

                Iterator<Object> iterator = ((List) object).iterator();
                while (iterator.hasNext()) {
                    jsonArray.put(Haru.encode(iterator.hasNext()));
                }
                return jsonArray;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        throw new IllegalArgumentException("The given object is not encodable.");
    }

    public static String getAppKey() {
        return mAppKey;
    }

    public static String getSdkKey() {
        return mSdkKey;
    }
}
