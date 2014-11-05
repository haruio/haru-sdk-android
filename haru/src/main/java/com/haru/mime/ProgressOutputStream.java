package com.haru.mime;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 파일 업로드 진행상황을 알 수 있는 OutputStream이다.
 */
public class ProgressOutputStream extends FilterOutputStream {
    private final ProgressListener listener;
    private long totalSize, transferred;

    public ProgressOutputStream(OutputStream outstream,
                                long totalSize,
                                ProgressListener progressListener) {
        super(outstream);
        this.totalSize = totalSize;
        this.listener = progressListener;
        transferred = 0;
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        out.write(buffer, offset, length);

        // Progress를 기록하고 핸들러를 호출한다.
        this.transferred += length;
        this.listener.progress(totalSize, transferred);
    }

    public interface ProgressListener {
        public abstract void progress(long total, long transferred);
    }
}
