package com.haru.examplememo;

import android.app.Application;
import android.util.Log;

import com.haru.Haru;
import com.haru.Installation;
import com.haru.push.Push;

public class App extends Application {

    private static final String APP_KEY = "934b90c0-20e5-40f4-94e7-31c05840ec83";
    private static final String SDK_KEY = "SDK_KEY_HERE";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Haru", "===========================================================================");

        Haru.init(this, APP_KEY, SDK_KEY);
        Push.initialize(this);
    }
}
