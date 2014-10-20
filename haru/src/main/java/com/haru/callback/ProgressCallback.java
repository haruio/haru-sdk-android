package com.haru.callback;

import com.haru.HaruException;

public interface ProgressCallback extends SaveCallback {
    public void progress(int percentage);
}
