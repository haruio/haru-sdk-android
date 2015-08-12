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
        Haru.init(this,  "9552b75d-5eb3-4474-870f-8fde67ac761f", "1158513e-6742-456a-8cc9-6dd3dfa060c3","http://api.haru.io/1");
        Installation.init(this);
        Push.init(this, "tcp://52.68.245.47:1883");

        Push.subscribe("testChannel");

        Config.loadInBackground();
    }
}
