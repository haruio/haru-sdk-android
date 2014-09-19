package com.haru.write;

import com.haru.Encodable;
import com.haru.Haru;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class Operation<T> implements Encodable {

    protected List<T> objects = new ArrayList<T>();

    protected Operation(T object) {
        objects.add(object);
    }

    public abstract String getMethod();

    public abstract void mergeFromPrevious(Operation other);

    @Override
    public Object encode() throws Exception {
        return Haru.encode(objects);
    }
}
