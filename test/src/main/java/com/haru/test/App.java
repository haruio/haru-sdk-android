package com.haru.test;

import android.app.Application;

import com.haru.Haru;
import com.haru.Installation;
import com.haru.push.Push;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Haru.initialize(this, "APP_KEY_HERE", "SDK_KEY_HERE");
        Installation.saveCurrentInstallationInBackground();

        Push.initialize(this);
    }
}
