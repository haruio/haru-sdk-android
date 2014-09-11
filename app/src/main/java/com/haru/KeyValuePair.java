package com.haru;

import org.json.JSONException;
import org.json.JSONObject;

public class KeyValuePair implements Encodable {
    private String key;
    private String value;

    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Object encode() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(key, value);
        return object;
    }
}
