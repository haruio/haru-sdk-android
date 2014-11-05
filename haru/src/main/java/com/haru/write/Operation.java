package com.haru.write;

import com.haru.JsonEncodable;
import com.haru.Haru;

import java.util.ArrayList;
import java.util.List;

public abstract class Operation<T> implements JsonEncodable {

    protected List<T> objects = new ArrayList<T>();

    protected Operation(T object) {
        objects.add(object);
    }

    public abstract String getMethod();

    public abstract void mergeFromPrevious(Operation other);

    @Override
    public Object toJson() throws Exception {
        return Haru.encode(objects);
    }

}
