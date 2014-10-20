package com.haru;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.haru.task.Continuation;
import com.haru.task.Task;

import java.util.UUID;

public class Installation extends Entity {

    private static String mUUID;
    private static Installation currentInstallation;

    public static void init(Context context) {
        currentInstallation = new Installation();
        currentInstallation.put("deviceType", "android");
        currentInstallation.put("deviceToken", getUuid(context));
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

        HaruRequest.Param param = new HaruRequest.Param();
        param.put("deviceType", currentInstallation.getString("deviceType"));
        param.put("deviceToken", currentInstallation.getString("deviceToken"));

        // 일단은 무조건 생성
        Task<HaruResponse> newInstallationTask = Haru.newWriteRequest("/installation")
                .post(param)
                .executeAsync();

        newInstallationTask.onSuccess(new Continuation<HaruResponse, Void>() {
            @Override
            public Void then(Task task) throws Exception {

                HaruResponse response = (HaruResponse) task.getResult();
                Log.e("Haru", "deviceUuid -> " + mUUID);
                Log.e("Haru", "saveCurrentInstallationInBackground -> " + response.getJsonBody().toString());


                return null;
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task task) throws Exception {
                return null;
            }
        });

    }

    public static Installation getCurrentInstallation() {
        return currentInstallation;
    }

}
