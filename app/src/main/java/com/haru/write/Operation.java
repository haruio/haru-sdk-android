package com.haru.write;

import com.haru.Haru;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class Operation<T> {

    protected List<T> objects = new ArrayList<T>();

    public Operation(T object) {
        objects.add(object);
    }

    protected abstract String getMethod();

    public JSONObject describeToJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("method", getMethod());
        object.put("objects", Haru.encode(objects));

        return object;
    }
}
