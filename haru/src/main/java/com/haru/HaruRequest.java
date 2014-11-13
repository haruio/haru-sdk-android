package com.haru;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.haru.callback.ResponseCallback;
import com.haru.mime.HttpMultipartMode;
import com.haru.mime.MultipartEntity;
import com.haru.mime.ProgressOutputStream;
import com.haru.mime.content.FileBody;
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
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.Callable;

public class HaruRequest {

    private static final String HARU_ENDPOINT = "http://api.haru.io/1";

    private static final String USER_AGENT =
            "Haru SDK " + Haru.getSdkVersion() + " / Android " + Build.VERSION.RELEASE;

    private static HttpClient defaultClient;
    private static String appKey, sdkKey;

    private HttpClient client;

    private String endpoint;
    private int method;
    private JSONObject param;
    private Param getParam;

    private File file;
    private ProgressOutputStream.ProgressListener progressListener;

    /**
     * HaruRequest를 초기화시킨다.
     */
    static void initialize(Context context) {
        if (defaultClient == null) {
            defaultClient = initiateApacheHttpClient(context);
        }

        appKey = Haru.getAppKey();
        sdkKey = Haru.getSdkKey();
    }

    /**
     * HaruRequest의 기본값으로 HTTP Client를 생성한다.
     * @return HTTP Client
     */
    private static HttpClient initiateApacheHttpClient(Context context) {
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

        return new DefaultHttpClient(new ThreadSafeClientConnManager(params,
                new DefaultHttpClient().getConnectionManager().getSchemeRegistry()), params);
    }

    public HaruRequest() {
        client = defaultClient;
    }

    public HaruRequest(String url) {
        this();
        endpoint = HARU_ENDPOINT + url;
    }

    /**
     * API 요청을 비동기식으로 실행한다.
     * Task를 반환하므로 .continueWith나 .onSuccess등의 함수를 chain해서 이후 작업을 지정할 수 있다.
     * @see com.haru.task.Task
     *
     * @return {@link com.haru.task.Task}
     */
    public Task<HaruResponse> executeAsync() {

        // not initialized?
        if (appKey == null) {
            throw new RuntimeException("You need to call Haru.init() before using other APIs.");
        }

        return Task.callInBackground(new Callable<HaruResponse>() {
            @Override
            public HaruResponse call() throws Exception {
                HttpUriRequest request;

                Haru.logD("Request URL => %s", endpoint);

                // Write HTTP Request
                switch(method) {
                    case 0: // GET
                        String url = endpoint;
                        if (getParam != null) url += "?" + getParam.toUrl();
                        Log.d("Haru", "  GET Requests => " + url);
                        request = new HttpGet(url);
                        break;

                    case 1: // POST
                        request = new HttpPost(endpoint);

                        // Multipart Upload인가?
                        if (file != null) {
                            MultipartEntity multipart = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                            multipart.addPart("file", new FileBody(file));

                            // Progress Callback 설정
                            if (progressListener != null) multipart.setProgressListener(progressListener);

                            ((HttpPost) request).setEntity(multipart);

                        } else {
                            Log.d("Haru", "Request => " + param.toString());
                            ((HttpPost) request).setEntity(new StringEntity(param.toString(), "utf-8"));
                        }
                        break;

                    case 2: // PUT
                        request = new HttpPut(endpoint);
                        ((HttpPut) request).setEntity(new StringEntity(param.toString(), "utf-8"));
                        break;

                    case 3: // DELETE
                        request = new HttpDelete(endpoint);
                        break;

                    default: // can't be possible!
                        throw new RuntimeException("method " + method + " does not exist!");
                }

                // Haru API Header
                request.setHeader("Application-Id", appKey);
                request.setHeader("Android-API-Id", sdkKey);

                if (file == null) {
                    // Sending Standard JSON Requests
                    request.setHeader("Accept", "application/json");
                    request.setHeader("Accept-Encoding", "utf-8");
                    request.setHeader("Content-Type", "application/json");
                }

                // Session Token
                if (User.getCurrentSessionToken() != null) {
                    request.setHeader("Session-Token", User.getCurrentSessionToken());
                }

                HttpResponse response = client.execute(request);

                // Read the response
                String body = EntityUtils.toString(response.getEntity(), "utf-8");
                Haru.logD(endpoint + " Response => " + body);
//              Haru.logD("Response => " + body);

                return new HaruResponse(response, new JSONObject(body));

            }
        }).continueWith(new Continuation<HaruResponse, HaruResponse>() {
            @Override
            public HaruResponse then(Task<HaruResponse> task) throws Exception {
                if (task.isFaulted()) throw task.getError();
                return task.getResult();
            }
        }, Task.UI_THREAD_EXECUTOR);
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
        getParam = param;
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

    public HaruRequest post(File file) {
        setMethod(1);
        this.file = file;
        return this;
    }

    public HaruRequest fileProgress(ProgressOutputStream.ProgressListener listener) {
        this.progressListener = listener;
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
        this.param = (JSONObject) param.toJson();
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
