package com.haru;

/**
 * JSON 형식으로 인코딩되어야 하는 데이터 클래스에 사용된다.
 */
public interface JsonEncodable {
    /**
     * 객체를 JSON으로 Marshalling한다.
     *
     * @return JSON ({@link org.json.JSONObject or @link org.json.JSONArray}
     * @throws Exception usually {@link org.json.JSONException}
     */
    public Object toJson() throws Exception;
}
