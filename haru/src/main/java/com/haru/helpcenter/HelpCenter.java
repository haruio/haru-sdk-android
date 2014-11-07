package com.haru.helpcenter;

import com.haru.Haru;
import com.haru.HaruException;
import com.haru.HaruResponse;
import com.haru.callback.SaveCallback;
import com.haru.helpcenter.callback.GetNoticeCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * HelpCenter 기능들을 사용할 수 있게 해주는 API이다.
 */
public class HelpCenter {

    /**
     * 공지사항들을 가져온다.
     * @param callback GetNoticeCallback
     * @return Task
     */
    public static Task getNoticeList(final GetNoticeCallback callback) {

        // request to help center server
        Task<HaruResponse> getTask = Haru.helpCenterRequest("/notice/list")
                .get()
                .executeAsync();

        return getTask.continueWith(new Continuation<HaruResponse, List<Notice>>() {
            @Override
            public List<Notice> then(Task<HaruResponse> task) throws Exception {
                if (task.isFaulted()) {
                    Haru.stackTrace(task.getError());
                    callback.done(null, new HaruException(task.getError()));
                    throw task.getError();

                } else if (task.getResult().hasError()) {
                    Exception e = task.getResult().getError();
                    Haru.stackTrace(e);
                    callback.done(null, new HaruException(e));
                    throw e;
                }

                // Get result
                HaruResponse response = task.getResult();
                JSONArray resultsArray = response.getJsonBody().getJSONArray("results");

                // Make list
                ArrayList<Notice> noticeList = new ArrayList<Notice>();
                for (int i = 0; i < resultsArray.length(); i++) {
                    noticeList.add(Notice.fromJson(resultsArray.getJSONObject(i)));
                }

                // callback
                callback.done(noticeList, null);

                return noticeList;
            }
        });
    }
/*
    public static Task sendQuestion(String email, String question, String category, String final SaveCallback callback) {

        // request to help center server
        Task<HaruResponse> getTask = Haru.helpCenterRequest("/notice/list")
                .get()
                .executeAsync();

        return getTask.continueWith(new Continuation<HaruResponse, List<Notice>>() {
            @Override
            public List<Notice> then(Task<HaruResponse> task) throws Exception {
                if (task.isFaulted()) {
                    Haru.stackTrace(task.getError());
                    callback.done(null, new HaruException(task.getError()));
                    throw task.getError();

                } else if (task.getResult().hasError()) {
                    Exception e = task.getResult().getError();
                    Haru.stackTrace(e);
                    callback.done(null, new HaruException(e));
                    throw e;
                }

                // Get result
                HaruResponse response = task.getResult();
                JSONArray resultsArray = response.getJsonBody().getJSONArray("results");

                // Make list
                ArrayList<Notice> noticeList = new ArrayList<Notice>();
                for (int i = 0; i < resultsArray.length(); i++) {
                    noticeList.add(Notice.fromJson(resultsArray.getJSONObject(i)));
                }

                // callback
                callback.done(noticeList, null);

                return noticeList;
            }
        });    }*/
}
