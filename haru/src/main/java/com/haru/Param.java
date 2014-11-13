package com.haru;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * POST, PUT, DELETE 메서드의 파라미터를 작성할 때 사용된다.
 */
public class Param implements JsonEncodable {
    private HashMap<String, Object> paramMap;

    public Param() {
        paramMap = new HashMap<String, Object>();
    }

    public void put(String key, Object value) {
        paramMap.put(key, value);
    }

    public void put(String key, Param value) {
        paramMap.put(key, value.toJson().toString());
    }

    public void put(String key, List value) {
        put(key, new JSONArray(value).toString());
    }

    /**
     * 파라미터들을 URL Encoding된 포맷으로 변환한다. (param1=name&param2=name)
     * @return String (URL Encoded UTF-8)
     */
    String toUrl() {
        try {
            StringBuilder query = new StringBuilder();
            Iterator iter = paramMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                query.append(entry.getKey() + "="
                        + URLEncoder.encode((String) entry.getValue(), "utf-8") + "&");
            }
            return query.toString();

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 파라미터들을 JSONObject로 변환한다.
     * @return JSONObject
     */
    @Override
    public Object toJson() {
        return new JSONObject(paramMap);
    }

    public static Param fromJson(JSONObject json) {
        Param param = new Param();
        param.paramMap = Haru.convertJsonToMap(json);
        return param;
    }
}
