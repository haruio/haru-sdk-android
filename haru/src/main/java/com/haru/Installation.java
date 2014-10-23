package com.haru;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.haru.task.Continuation;
import com.haru.task.Task;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Installation extends Entity {

    private static String mUUID;
    private static Installation currentInstallation;

    // Non-static members
    private ArrayList<String> channels;

    public static void init(Context context) {
        currentInstallation = new Installation();
        currentInstallation.put("deviceType", "android");
        currentInstallation.put("deviceToken", getUuid(context));
        currentInstallation.put("pushType", "mqtt");

        // TODO: DEbug
        currentInstallation.channels.add("testChannel");
    }

    private static String getUuid(Context context) {
        if (mUUID == null) {
            TelephonyManager tm =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            String tmDevice, tmSerial, androidId;
            tmDevice = "" + tm.getDeviceId();
            tmSerial = "" + tm.getSimSerialNumber();
            androidId = "" +
                    android.provider.Settings.Secure.getString(context.getContentResolver(),
                            android.provider.Settings.Secure.ANDROID_ID);

            UUID deviceUuid = new UUID(androidId.hashCode(),
                    ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
            mUUID = deviceUuid.toString();
        }
        return mUUID;
    }

    public static void saveCurrentInstallationInBackground() {
        currentInstallation.saveInBackground();
    }

    public static Installation getCurrentInstallation() {
        return currentInstallation;
    }


    public Installation() {
        super();
        channels = new ArrayList<String>();
    }

    public void addChannel(String channel) {
        channels.add(channel);
    }

    public void addChannels(List<String> channels) {
        channels.addAll(channels);
    }

    public void clearChannels() {
        channels.clear();
    }

    public List<String> getChannels() {
        return channels;
    }

    public Task saveInBackground() {
        HaruRequest.Param param = new HaruRequest.Param();
        param.put("deviceType", getString("deviceType"));
        param.put("deviceToken", getString("deviceToken"));
        param.put("pushType", getString("pushType"));

        param.put("channels", channels);

        // 일단은 무조건 생성
        Task<HaruResponse> newInstallationTask = Haru.newUserRequest("/installation")
                .post(param)
                .executeAsync();

        return newInstallationTask.onSuccess(new Continuation<HaruResponse, Installation>() {
            @Override
            public Installation then(Task task) throws Exception {

                HaruResponse response = (HaruResponse) task.getResult();
                Log.e("Haru", "deviceUuid -> " + mUUID);
                Log.e("Haru", "saveCurrentInstallationInBackground -> " + response.getJsonBody().toString());

                return Installation.this;
            }
        }).continueWith(new Continuation<Installation, Installation>() {
            @Override
            public Installation then(Task task) throws Exception {
                return Installation.this;
            }
        });
    }


}
