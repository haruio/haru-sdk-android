package com.haru.test;

import android.app.Application;
import android.util.Log;

import com.haru.Config;
import com.haru.Haru;
import com.haru.push.Push;

public class App extends Application {

    private static final String APP_KEY = "c64caf78-2ff2-4ec9-b006-11d14184c96a";
    private static final String SDK_KEY = "f3c1757c-57f7-4f61-8b31-8cc78c6b638c";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Haru", "===========================================================================");
        Haru.init(this, APP_KEY, SDK_KEY);
        Push.subscribe("testChannel");
        Push.init(this);
        Config.loadInBackground();
    }
}
