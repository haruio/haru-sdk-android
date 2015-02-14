package com.haru.test;

import android.app.Application;
import android.util.Log;

import com.haru.Config;
import com.haru.Haru;
import com.haru.Installation;
import com.haru.push.Push;

public class App extends Application {

    private static final String APP_KEY = "bc743677-923f-4177-ad1a-8dc36df1516b";
    private static final String SDK_KEY = "1348c789-46d4-4d25-8f51-4192b7285c60";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Haru", "===========================================================================");
        Haru.init(this, APP_KEY, SDK_KEY,  "http://54.65.126.198/1");
        Installation.init(this);
        Push.init(this, "tcp://54.65.101.20:1884");

        Push.subscribe("testChannela");

        Config.loadInBackground();
    }
}
