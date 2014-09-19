package com.haru;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Haru API 호출 결과를 담는 클래스이다.
 */
public class HaruResponse {
    private int statusCode;
    private JSONObject jsonResult;
    private HashMap<String, String> headers;

    private HaruException exception;
    private boolean hasError = false;

    HaruResponse(HttpResponse response, JSONObject result) {
        statusCode = response.getStatusLine().getStatusCode();
        jsonResult = result;
        headers = convertHeaders(response.getAllHeaders());

        try {
            if (jsonResult.has("code") && jsonResult.has("error")) {
                hasError = true;
                exception = new HaruException(
                        jsonResult.getInt("code"),
                        jsonResult.getString("error"));
            }
        } catch (JSONException e) {
            throw new RuntimeException("JSON parse failed");
        }
    }


    /**
     * 호출 결과를 JSON Object 형태로 반환한다.
     * @return JSONObject
     */
    public JSONObject getJsonBody() {
        return jsonResult;
    }

    /**
     * Response의 HTTP 헤더를 해시맵 형태로 반환한다.
     */
    public HashMap<String, String> getHeaders() {
        return headers;
    }

    /**
     * HTTP Response 상태 코드를 반환한다.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * API 호출이 실패했나?
     * @return 실패 여부
     */
    public boolean hasApiError() {
        return hasError;
    }

    /**
     * API 호출에 실패했을 시, 해당 내용이 담긴 HaruException을 반환한다.
     * (아닐 시 null 반환)
     *
     * @return 상태 코드와 에러 메세지가 담긴 HaruException
     */
    public HaruException getApiError() {
        return exception;
    }

    private HashMap<String, String> convertHeaders(Header[] headers) {
        HashMap<String, String> result = new HashMap<String, String>(headers.length);
        for (Header header : headers) {
            result.put(header.getName(), header.getValue());
        }
        return result;
    }
}
