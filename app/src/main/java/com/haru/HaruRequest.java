package com.haru;

import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class HaruRequest {

    private HttpClient client;
    private HttpUriRequest request;

    private static final String USER_AGENT = "Haru Android";
    private String endpoint;
    private JSONObject params;

    private static HttpClient newHttpClient() {

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
        client = newHttpClient();
    }

    public HaruRequest(String url) {
        this();
        endpoint = url;
    }

    public void setUrl(String url) { endpoint = url; }

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
