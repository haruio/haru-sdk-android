package com.haru.write;

import com.haru.Encodable;
import com.haru.Haru;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OperationSet extends HashMap<String, Operation> implements Encodable {

    @Override
    public Object encode() throws Exception {
        JSONArray jsonArray = new JSONArray();
        Iterator<Map.Entry<String, Operation>> iter = this.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Operation> pair = iter.next();
            JSONObject json = new JSONObject();
            json.put("method", pair.getKey());
            json.put("fields", Haru.encode(pair.getValue()));
            jsonArray.put(json);
        }
        return jsonArray;
    }
}
