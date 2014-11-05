package com.haru;

import com.haru.callback.SaveCallback;
import com.haru.callback.SaveWithProgressCallback;
import com.haru.mime.ProgressOutputStream;
import com.haru.task.Continuation;
import com.haru.task.Task;

import org.json.JSONObject;

import java.io.File;
import java.util.Date;

/**
 * Haru 서버에 업로드된 파일에 대한 정보이다.
 * An abstract representation of file in Haru server.
 */
public class HaruFile implements JsonEncodable {

    // 파일에 대한 정보 (From server)
    private String url, fileId;
    private Date createdAt, updatedAt;

    // 실제 다운로드된 (로컬에 있는) 파일에 대한 정보
    private File file;

    // 상태
    private boolean isAccessible, isInServer;

    /**
     * 새로운 파일을 생성한다. 아직 서버에 올라가있진 않은 상태이다.
     * @param path 파일 경로
     */
    public HaruFile(String path) {
        this(new File(path));
    }

    /**
     * 새로운 파일을 생성한다.
     * @param file
     */
    public HaruFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null.");

        } else if (!file.exists()) {
            throw new RuntimeException("No such file : " + file.getAbsolutePath());
        }

        this.file = file;
        this.isAccessible = true;
    }

    public Task saveInBackground() {
        return Haru.newWriteRequest("/file")
                .post(file)
                .executeAsync()
                .continueWith(new Continuation<HaruResponse, HaruFile>() {
                    @Override
                    public HaruFile then(Task<HaruResponse> task) throws Exception {

                        return null;
                    }
                });
    }

    public Task saveInBackground(SaveCallback saveCallback) {

        HaruRequest request = Haru.newFileRequest("/file")
                .post(file);

        if (saveCallback instanceof SaveWithProgressCallback) {
            final SaveWithProgressCallback progressCallback = (SaveWithProgressCallback) saveCallback;
            ProgressOutputStream.ProgressListener progressListener =
                    new ProgressOutputStream.ProgressListener() {
                        @Override
                        public void progress(long total, long transferred) {
                            progressCallback.progress((double) transferred / total * 100);
                        }
                    };

            request.fileProgress(progressListener);
        }

        return request.executeAsync().continueWith(new Continuation<HaruResponse, HaruFile>() {
            @Override
            public HaruFile then(Task<HaruResponse> task) throws Exception {
                if (task.isFaulted()) {
                    Haru.stackTrace(task.getError());
                    throw task.getError();
                }

                // Fill the file infomration
                JSONObject body = task.getResult().getJsonBody();
                HaruFile.this.fileId = body.getString("_id");
                HaruFile.this.createdAt = new Date(body.getLong("createdAt"));
                HaruFile.this.updatedAt = new Date(body.getLong("updatedAt"));
                HaruFile.this.url = body.getString("url");

                return HaruFile.this;
            }
        });
    }

    // TODO : For furture integration to Entity
    @Override
    public Object toJson() throws Exception {
        return null;
    }
}
