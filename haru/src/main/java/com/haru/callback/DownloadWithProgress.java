package com.haru.callback;

public interface DownloadWithProgress extends SaveCallback {
    public void progress(double percentage);
}
