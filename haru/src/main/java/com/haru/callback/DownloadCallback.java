package com.haru.callback;

import com.haru.HaruException;
import com.haru.HaruFile;

public interface DownloadCallback {
    public void done(HaruFile file, HaruException error);
}
