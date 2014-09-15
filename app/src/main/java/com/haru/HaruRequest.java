package com.haru;

import android.content.Context;

import com.haru.internal.Task;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Callable;

public class HaruRequest {

    private static final String USER_AGENT = "Haru Android";

    private static HttpClient defaultClient;
    private static String appKey, sdkKey;

    private HttpClient client;
    private HttpUriRequest request;

    private String endpoint;
    private int method;
    private JSONObject params;

    /**
     * HaruRequest를 초기화시킨다.
     * @param context Application Context
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
     * @param context Application Context
     * @return HTTP Client
     */
    private static HttpClient newHttpClient(Context context) {
        HttpParams params = new BasicHttpParams();

        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.setSoTimeout(params, 10000);
        HttpConnectionParams.setSocketBufferSize(params, 8192);

        HttpClientParams.setRedirecting(params, false);

        HttpProtocolParams.setUserAgent(params, USER_AGENT);

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

    public HaruRequest(int method, String url) {
        client = defaultClient;
        endpoint = url;
        this.method = method;
    }

    public Task<JSONObject> execute() {
        return Task.call(new Callable<JSONObject>() {
            @Override
            public JSONObject call() throws Exception {
                HttpResponse response = client.execute(request);
                return null;
            }
        });
    }

    public void setUrl(String url) {
        endpoint = url;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    void put(String key, String value) {
        try {
            this.params.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    void put(String key, int value) {
        try {
            this.params.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    void put(String key, long value) {
        try {
            this.params.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    void put(String key, JSONArray value) {
        try {
            this.params.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    void put(String key, JSONObject value) {
        try {
            this.params.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
