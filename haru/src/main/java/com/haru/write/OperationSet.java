package com.haru.write;

import com.haru.JsonEncodable;
import com.haru.Haru;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OperationSet extends HashMap<String, Operation> implements JsonEncodable {

    private String className, entityId;
    public OperationSet(String className, String entityId) {
        this.className = className;
        this.entityId = entityId;
    }

    @Override
    public Object toJson() throws Exception {
        JSONArray jsonArray = new JSONArray();
        Iterator<Map.Entry<String, Operation>> iter = this.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Operation> pair = iter.next();
            Operation operation = pair.getValue();

            JSONObject json = new JSONObject();
            json.put("method", operation.getMethod());
            json.put("class", className);
            json.put("_id", entityId);
            json.put(operation.getRequestDataKey(), Haru.encode(operation));
            jsonArray.put(json);
        }
        return jsonArray;
    }
}
