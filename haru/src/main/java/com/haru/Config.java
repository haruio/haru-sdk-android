package com.haru;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.haru.callback.LoadCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Pre-defined configurations
 * from Haru Dashboard > Settings > App Config page.
 */
public class Config {

    // we use preference xml to store configuration
    private static String XML_NAME = "_haru_config";
    private static String CONFIG_FIELD = "config";

    private static SharedPreferences pref;

    private static HashMap<String, Object> configMap;

    private static boolean isLoaded = false;
    private static Context context;

    static void init(Context ctx) {
        context = ctx;
        pref = context.getSharedPreferences(XML_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Load configurations from server.
     */
    public static void loadInBackground() {
        loadInBackground(null);
    }

    /**
     * Load configurations from server.
     * @param callback Callback to be called after the loading is finished
     */
    public static void loadInBackground(final LoadCallback callback) {

        // get connectivity manager to get network status
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo == null || !netInfo.isConnected()) {
            // check offline preferences
            if (!pref.contains(CONFIG_FIELD)) {
                // we can't reach it!
                String errorMsg = "Failed to load configuration - Network offline.";
                Haru.logI(errorMsg);
                if (callback != null) callback.done(new HaruException(errorMsg));

            } else {
                // It is in offline.
                try {
                    configMap = Haru.convertJsonToMap(
                            new JSONObject(pref.getString(CONFIG_FIELD, "{}")));

                } catch (JSONException e) {
                    Haru.stackTrace(e);
                    if (callback != null) callback.done(new HaruException(e));
                }
            }

        } else {
            // load configurations from server
            // and cache it to local using offline entity store.
            Task<HaruResponse> loadTask = new HaruRequest("/config").executeAsync();

            loadTask.continueWith(new Continuation<HaruResponse, Void>() {
                @Override
                public Void then(Task<HaruResponse> task) throws Exception {

                    // error handling
                    if (task.isFaulted()) {
                        if (callback != null) callback.done(new HaruException(task.getError()));
                        throw task.getError();
                    }

                    // fill the map
                    configMap = new HashMap<String, Object>();
                    JSONObject body = task.getResult().getJsonBody();
                    Iterator iter = body.keys();

                    while (iter.hasNext()) {
                        String key = (String) iter.next();
                        configMap.put(key, determineType(body.getJSONArray(key)));
                    }

                    // save it to the preference
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(CONFIG_FIELD, new JSONObject(configMap).toString());
                    editor.apply();

                    isLoaded = true;

                    // callback
                    if (callback != null) callback.done(null);

                    return null;
                }
            });
        }
    }

    /**
     * Determines the config value type and return.
     */
    private static Object determineType(JSONArray array) throws JSONException {
        String type = array.getString(0);
        if (type.equals("number")) return getNumber(array);
        else if (type.equals("boolean")) return array.getBoolean(1);
        else if (type.equals("string")) return array.getString(1);
        else if (type.equals("date")) return new Date(array.getLong(1));
        else if (type.equals("array")) return array.getJSONArray(1);
        else return array.get(1);
    }

    /**
     * Returns the correct type of Number field.
     * @param array Config's value array (0: type, 1: value)
     * @return a number value (Long || Int || Double)
     */
    private static Object getNumber(JSONArray array) throws JSONException {
        try {
            return array.getDouble(1);
        } catch (Exception e1) {
            try {
                return array.getLong(1);
            } catch (Exception e2) {
                try {
                    return array.getInt(1);
                } catch (Exception e3) {
                    return array.get(1);
                }
            }
        }
    }

    /**
     * Get integer value from configuration.
     * NOTE: Config should be loaded once before getting value!
     *
     * @param key Configuration field name
     * @return value (Integer)
     */
    public static int getInt(String key) {
        if (!isLoaded)
            throw new IllegalStateException("The Config should be loaded once before getting value!");
        return (Integer) configMap.get(key);
    }

    /**
     * Get double value from configuration.
     * NOTE: Config needs to be loaded before getting value!
     *
     * @param key Configuration field name
     * @return value (Double)
     */
    public static Double getDouble(String key) {
        if (!isLoaded)
            throw new IllegalStateException("The Config should be loaded once before getting value!");
        return (Double) configMap.get(key);
    }

    /**
     * Get long value from configuration.
     * NOTE: Config needs to be loaded before getting value!
     *
     * @param key Configuration field name
     * @return value (Long)
     */
    public static Long getLong(String key) {
        if (!isLoaded)
            throw new IllegalStateException("The Config should be loaded once before getting value!");
        return (Long) configMap.get(key);
    }

    /**
     * Get string value from configuration.
     * NOTE: Config needs to be loaded before getting value!
     *
     * @param key Configuration field name
     * @return value (Integer)
     */
    public static String getString(String key) {
        if (!isLoaded)
            throw new IllegalStateException("The Config should be loaded once before getting value!");
        return (String) configMap.get(key);
    }

    /**
     * Get All configurations.
     * @return Map - String, object pair
     */
    public Map<String, Object> getAll() {
        if (!isLoaded)
            throw new IllegalStateException("The Config should be loaded once before getting value!");
        return configMap;
    }
}
