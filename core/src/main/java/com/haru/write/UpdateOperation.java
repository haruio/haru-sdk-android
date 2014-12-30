package com.haru.write;

import com.haru.Haru;

import java.util.HashMap;
import java.util.Map;

public class UpdateOperation implements Operation {

    Map<String, Object> data;

    public UpdateOperation(String key, Object value) {
        data = new HashMap<String, Object>();
        data.put(key, value);
    }

    @Override
    public String getMethod() {
        return "update";
    }

    @Override
    public String getRequestDataKey() {
        return "entity";
    }

    @Override
    public void mergeFromPrevious(Operation other) {
        if (other instanceof UpdateOperation) {
            this.data.putAll(((UpdateOperation) other).data);
        }
    }

    public void removeOperationByKey(String key) {
        data.remove(key);
    }

    @Override
    public Object toJson() throws Exception {
        return Haru.encode(data);
    }
}
