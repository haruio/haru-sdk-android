package com.haru.callback;

import com.haru.HaruException;

public interface SaveWithProgressCallback extends SaveCallback {
    public void progress(double percentage);
}
