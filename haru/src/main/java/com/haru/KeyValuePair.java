package com.haru;

import org.json.JSONException;
import org.json.JSONObject;

public class KeyValuePair implements JsonEncodable {
    private String key;
    private Object value;

    public KeyValuePair(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public Object toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(key, value);
        return object;
    }
}
