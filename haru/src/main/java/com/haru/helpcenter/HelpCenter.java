package com.haru.helpcenter;

import com.haru.Haru;
import com.haru.HaruException;
import com.haru.HaruRequest;
import com.haru.HaruResponse;
import com.haru.Param;
import com.haru.callback.SaveCallback;
import com.haru.helpcenter.callback.GetCategoryCallback;
import com.haru.helpcenter.callback.GetFAQCallback;
import com.haru.helpcenter.callback.GetNoticeCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;

import org.json.JSONArray;
import org.json.JSONObject;

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
        Task<HaruResponse> getTask = new HaruRequest("/notice/list")
                .get()
                .executeAsync();

        return getTask.continueWith(new Continuation<HaruResponse, List<Notice>>() {
            @Override
            public List<Notice> then(Task<HaruResponse> task) throws Exception {

                // error handling
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
                JSONArray resultsArray = response.getJsonBody().getJSONArray("return");

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

    public static Task sendQuestion(String email,
                                    String category,
                                    String question,
                                    final SaveCallback callback) {

        Param param = new Param();
        param.put("emailaddress", email);
        param.put("category", category);
        param.put("body", question);

        // request to help center server
        Task<HaruResponse> addQnaTask = new HaruRequest("/qna/add")
                .post(param)
                .executeAsync();

        return addQnaTask.continueWith(new Continuation<HaruResponse, HaruResponse>() {
            @Override
            public HaruResponse then(Task<HaruResponse> task) throws Exception {

                // error handling
                if (task.isFaulted()) {
                    Haru.stackTrace(task.getError());
                    callback.done(new HaruException(task.getError()));
                    throw task.getError();

                } else if (task.getResult().hasError()) {
                    Exception e = task.getResult().getError();
                    Haru.stackTrace(e);
                    callback.done(new HaruException(e));
                    throw e;
                }

                // callback
                callback.done(null);

                return task.getResult();
            }
        });
    }

    public static Task getFrequentlyAskedQuestions(String categoryName,
                                                   final GetFAQCallback callback) {

        // request to help center server
        Task<HaruResponse> getTask = new HaruRequest("/faq/list/" + categoryName)
                .get()
                .executeAsync();

        return getTask.continueWith(new Continuation<HaruResponse, List<FAQ>>() {
            @Override
            public List<FAQ> then(Task<HaruResponse> task) throws Exception {

                // error handling
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
                JSONArray resultsArray = response.getJsonBody().getJSONArray("return");

                // Make list
                ArrayList<FAQ> faqList = new ArrayList<FAQ>();
                for (int i = 0; i < resultsArray.length(); i++) {
                    faqList.add(FAQ.fromJson(resultsArray.getJSONObject(i)));
                }

                // callback
                callback.done(faqList, null);

                return faqList;
            }
        });
    }

    public static Task getFaqCategories(final GetCategoryCallback callback) {
        // request to help center server
        Task<HaruResponse> getTask = new HaruRequest("/faq/category/list")
                .get()
                .executeAsync();

        return getTask.continueWith(new Continuation<HaruResponse, List<String>>() {
            @Override
            public List<String> then(Task<HaruResponse> task) throws Exception {

                // error handling
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
                JSONArray resultsArray = response.getJsonBody().getJSONArray("return");

                // Make list
                ArrayList<String> categories = new ArrayList<String>();
                for (int i = 0; i < resultsArray.length(); i++) {
                    categories.add(resultsArray.getJSONObject(i).getString("Category"));
                }

                // callback
                callback.done(categories, null);

                return categories;
            }
        });
    }
}
