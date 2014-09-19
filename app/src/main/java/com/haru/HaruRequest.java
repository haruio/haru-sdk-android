package com.haru;

import android.content.Context;
import com.haru.callback.ResponseCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.concurrent.Callable;

public class HaruRequest {

    private static HttpClient defaultClient;
    private static String appKey, sdkKey;

    private HttpClient client;

    private String endpoint;
    private int method;
    private JSONObject param;

    /**
     * POST, PUT, DELETE 메서드의 파라미터를 작성할 때 사용된다.
     */
    public class Param {
        private HashMap<String, String> paramMap = new HashMap<String, String>();

        public void put(String key, String value) {
            paramMap.put(key, value);
        }

        public void put(String key, int value) {
            put(key, String.valueOf(value));
        }

        public void put(String key, double value) {
            put(key, String.valueOf(value));
        }

        public void put(String key, boolean value) {
            put(key, String.valueOf(value));
        }

        public void put(String key, long value) {
            put(key, String.valueOf(value));
        }

        /**
         * 파라미터들을 JSONObject로 변환한다.
         * @return JSONObject
         */
        JSONObject toJSON() {
            return new JSONObject(paramMap);
        }
    }

    /**
     * HaruRequest를 초기화시킨다.
     */
    static void initialize(Context context) {
        if (defaultClient == null) {
            defaultClient = newHttpClient(context);
        }

        appKey = Haru.getAppKey();
        sdkKey = Haru.getSdkKey();
    }

    /**
     * HaruRequest의 기본값으로 HTTP Client를 생성한다.
     * @return HTTP Client
     */
    private static HttpClient newHttpClient(Context context) {
        HttpParams params = new BasicHttpParams();

        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.setSoTimeout(params, 10000);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        HttpClientParams.setRedirecting(params, false);

        ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(20));
        ConnManagerParams.setMaxTotalConnections(params, 20);

        String host = System.getProperty("http.proxyHost");
        String portString = System.getProperty("http.proxyPort");
        if ((host != null) && (host.length() != 0) && (portString != null) && (portString.length() != 0)) {
            int port = Integer.parseInt(portString);
            HttpHost proxy = new HttpHost(host, port, "http");
            params.setParameter("http.route.default-proxy", proxy);
        }

        return new DefaultHttpClient(params);
    }

    public HaruRequest() {
        client = defaultClient;
    }

    public HaruRequest(String url) {
        client = defaultClient;
        endpoint = url;
    }

    /**
     * API 요청을 비동기식으로 실행한다.
     * Task를 반환하므로 .continueWith나 .onSuccess등의 함수를 chain해서 이후 작업을 지정할 수 있다.
     * @see com.haru.task.Task
     *
     * @return {@link com.haru.task.Task}
     */
    public Task<HaruResponse> executeAsync() {
        return Task.call(new Callable<HaruResponse>() {
            @Override
            public HaruResponse call() throws Exception {
                HttpUriRequest request;

                // Write HTTP Request
                switch(method) {
                    case 0: // GET
                        request = new HttpGet(endpoint);
                        break;

                    case 1: // POST
                        request = new HttpPost(endpoint);
                        ((HttpPost) request).setEntity(new StringEntity(param.toString()));
                        break;

                    case 2: // PUT
                        request = new HttpPut(endpoint);
                        ((HttpPut) request).setEntity(new StringEntity(param.toString()));
                        break;

                    case 3: // DELETE
                        request = new HttpDelete(endpoint);
                        break;

                    default: // can't be possible!
                        throw new RuntimeException("method " + method + " does not exist!");
                }

                // Sending JSON Data
                request.setHeader("Accept", "application/json");
                request.setHeader("Content-Type", "application/json");

                // Haru API Header
                request.setHeader("Application-Id", appKey);
                request.setHeader("Android-API-Id", sdkKey);

                HttpResponse response = client.execute(request);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), "utf-8"));

                // Read the response
                String line;
                StringBuilder result = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    result.append(line);
                }

                return new HaruResponse(response, new JSONObject(result.toString()));
            }
        });
    }

    public void executeAsync(final ResponseCallback callback) {
        executeAsync().continueWith(new Continuation<HaruResponse, Object>() {
            @Override
            public Object then(Task<HaruResponse> task) throws Exception {
                callback.done(task.getResult());

                // continuation ends
                return null;
            }
        });
    }

    public HaruRequest get() {
        setMethod(0); // GET
        return this;
    }

    public HaruRequest get(Param param) {
        get();
        setParameter(param);
        return this;
    }


    public HaruRequest post(Param param) {
        setMethod(1); // POST
        setParameter(param);
        return this;
    }

    public HaruRequest post(JSONObject param) {
        setMethod(1); // POST
        setParameter(param);
        return this;
    }


    public HaruRequest put(Param param) {
        setMethod(2); // PUT
        setParameter(param);
        return this;
    }

    public HaruRequest delete() {
        setMethod(3); // DELETE
        return this;
    }

    public void setParameter(Param param) {
        this.param = param.toJSON();
    }

    public void setParameter(JSONObject param) {
        this.param = param;
    }

    /**
     * HTTP 메서드를 설정한다. 기본값은 GET이다.
     * @param method 메서드 (0: GET, 1: POST, 2: PUT, 3: DELETE)
     */
    public void setMethod(int method) {
        if (method < 0 || method > 3) {
            throw new IllegalArgumentException("method " + method + " does not exist!");
        }
        this.method = method;
    }
}
