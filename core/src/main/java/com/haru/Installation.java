package com.haru;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.haru.callback.SaveCallback;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * 앱 설치 정보이다. 앱이 설치된 이후, 처음 실행시 생성되어 서버에 저장된다.
 * <br/> 통계 및 푸시에 사용된다.
 */
@ClassNameOfEntity(Installation.CLASS_NAME)
public final class Installation extends Entity {

    public static final String CLASS_NAME = "Installations";
    private static final String CURRENT_INSTALLATION_TAG = "__currentInstallation";

    private static String mUUID;
    private static Context context;
    private static Installation currentInstallation;

    /**
     * Cannot initiate from other place - the installation must be only one.
     */
    Installation() {
        super(CLASS_NAME);
    }

    /**
     * Installation을 초기화하고, 현재 설치 정보를 구해온다.
     * @param appContext Application Context {@link android.app.Application}
     */
    public static void init(Context appContext) {
        context = appContext;

        // check already have installation in local
        ArrayList<Installation> entities =
                LocalEntityStore.retrieveEntitiesByTag(CLASS_NAME, CURRENT_INSTALLATION_TAG);

        if (entities == null || entities.size() == 0) {
            // this is the first time.
            currentInstallation = new Installation();
            Haru.logI("Making a new installation...");

            // update and save
            currentInstallation.fillInformation();
            currentInstallation.saveInBackground(new SaveCallback() {
                @Override
                public void done(HaruException error) {
                    if (error != null) {
                        Log.e("Haru", "An error occured when saving installation first time : " + error.getMessage());
                        error.printStackTrace();
                        return;
                    }

                    // save in local
                    LocalEntityStore.saveEntity(currentInstallation, CURRENT_INSTALLATION_TAG);
                }
            });

        } else {
            // use the old one.
            currentInstallation = entities.get(0);
            Haru.logI("Installation already exists (%s)", currentInstallation.entityId);

            // update and save
//          currentInstallation.fillInformation();
//          currentInstallation.saveInBackground();
        }
    }

    void fillInformation() {
        put("deviceType", "android");
        put("pushType", "mqtt");

        updateTimezone();
        updateVersionInfo();
        updateUuid();
        updateNation();
        updateModelName();
    }

    private String getLanguage(Context con) {
        String TAG = "Installation";
        Log.d(TAG, "getLanguage() Start");
        String systemLanguage = con.getResources().getConfiguration().locale.getLanguage();

        if(systemLanguage.equals(Locale.KOREAN.toString())) {
            Log.d(TAG, "System language is KOREAN.");
        } else if(systemLanguage.equals(Locale.ENGLISH.toString())) {
            Log.d(TAG, "System language is ENGLISH.");
        } else if(systemLanguage.equals(Locale.JAPANESE.toString())) {
            Log.d(TAG, "System language is JAPANESE.");
        } else if(systemLanguage.equals(Locale.CHINESE.toString())) {
            Log.d(TAG, "System language is CHINESS.");
        } else if(systemLanguage.equals("in")) {
            Log.d(TAG, "System language is INDONESIAN.");
        } else {
            Log.d(TAG, "System language is other. set default(en)");
            systemLanguage = "en";
        }

        return systemLanguage;
    }

    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    private void updateModelName() {

        String code = getLanguage(context);//context.getResources().getConfiguration().locale.getCountry();
        Haru.logD("device --> %s", getDeviceName());
        super.put("device", getDeviceName());
    }
    private void updateNation() {

        String code = getLanguage(context);//context.getResources().getConfiguration().locale.getCountry();
        Haru.logD("nation --> %s", code);
        super.put("nation", code);
    }

    private void updateTimezone() {
        String zone = TimeZone.getDefault().getID();
        if (((zone.indexOf('/') > 0) || (zone.equals("GMT"))) && (!zone.equals(get("timeZone"))))
            super.put("timeZone", zone);
    }

    private void updateVersionInfo() {
        try {
            String packageName = context.getPackageName();
            PackageManager pm = context.getPackageManager();
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, 0);

            String appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
            String appVersion = pkgInfo.versionName;

            if ((packageName != null) && (!packageName.equals(get("appIdentifier")))) {
                super.put("appIdentifier", packageName);
            }
            if ((appName != null) && (!appName.equals(get("appName")))) {
                super.put("appName", appName);
            }
            if (((appVersion != null ? 1 : 0) & (!appVersion.equals(get("appVersion")) ? 1 : 0)) != 0)
                super.put("appVersion", appVersion);

            if (!Build.VERSION.RELEASE.equals(get("androidVersion"))) {
                super.put("androidVersion", Build.VERSION.RELEASE);
            }

        } catch (PackageManager.NameNotFoundException e) {
            Log.w("Haru", "Cannot load package information; will not saved in installation");
        }

        if (!Haru.getSdkVersion().equals(get("haruVersion")))
            super.put("haruVersion", Haru.getSdkVersion());
    }

    private void updateUuid() {
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
        put("deviceToken", mUUID);
        Haru.logI("deviceToken : %s", mUUID);
    }

    /**
     * 현재 이 앱의 설치 정보를 반환한다.
     * @return {@link com.haru.Installation}
     */
    public static Installation getCurrentInstallation() {
        return currentInstallation;
    }

    public void addPushChannel(String channel) {
        ArrayList<String> channels = getPushChannels();
        channels.add(channel);
        put("channels", channels);
    }

    public void addPushChannels(List<String> channels) {
        ArrayList<String> currentChannels = getPushChannels();
        currentChannels.addAll(channels);
        put("channels", currentChannels);
    }

    public void clearPushChannels() {
        put("channels", new ArrayList<String>());
    }

    public ArrayList<String> getPushChannels() {
        // TODO: Need to refactor
        try {
            ArrayList<String> channels;
            Object list = super.get("channels");
            if (list instanceof JSONArray) {
                channels = new ArrayList<String>();
                JSONArray listJson = (JSONArray) list;
                for (int i=0;i<listJson.length();i++) {
                    channels.add(listJson.getString(i));
                }
            }
            else channels = (ArrayList<String>) list;
            if (channels == null) channels = new ArrayList<String>();
            return channels;
        } catch (JSONException e) {
            Haru.stackTrace(e);
            return new ArrayList<String>();
        }
    }
}
