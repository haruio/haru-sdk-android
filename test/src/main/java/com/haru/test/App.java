package com.haru.test;

import android.app.Application;

import com.haru.Haru;
import com.haru.Installation;
import com.haru.push.Push;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Haru.initialize(this, "934b90c0-20e5-40f4-94e7-31c05840ec83", "SDK_KEY_HERE");
        Installation.saveCurrentInstallationInBackground();

        Push.initialize(this);
    }
}
