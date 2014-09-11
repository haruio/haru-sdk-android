package com.haru;

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

    public static void initialize(String appKey, String sdkKey) {
        mAppKey = appKey;
        mSdkKey = sdkKey;
    }

    public static Object encode(Object object) throws JSONException {

        if (object instanceof KeyValuePair) {
            JSONObject decoded = new JSONObject();
            decoded.put("key", ((KeyValuePair) object).getKey());
            decoded.put("value", ((KeyValuePair) object).getValue());
            return decoded;

        } else if (object instanceof List) {
            JSONArray array = new JSONArray();

            Iterator<Object> iterator = ((List) object).iterator();
            while (iterator.hasNext()) {
                array.put(encode(iterator.hasNext()));
            }

            return array;
        }

        return object;
    }
}
