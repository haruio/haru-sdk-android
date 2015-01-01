package com.haru.helpcenter;

import com.haru.Haru;
import com.haru.HaruException;
import com.haru.HaruRequest;
import com.haru.HaruResponse;
import com.haru.Param;
import com.haru.User;
import com.haru.callback.SaveCallback;
import com.haru.helpcenter.callback.GetCategoryCallback;
import com.haru.helpcenter.callback.GetFAQCallback;
import com.haru.helpcenter.callback.GetNoticeCallback;
import com.haru.task.Continuation;
import com.haru.task.Task;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * 고객 관리 및 고객과의 소통을 위한 고객센터 기능을 제공한다.
 * @author VISTA
 */
public class HelpCenter {

    /**
     * 공지사항들을 가져온다.
     * haru.io 관리자 페이지의 고객 관리 > 공지사항 메뉴에서 확인할 수 있다.
     *
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

    public static Task sendQuestion(String email, String question) {
        return sendQuestion(email, question, null);
    }

    /**
     * 질문을 보낸다.
     * haru.io 관리자 페이지의 고객 관리 > 질문사항 메뉴에서 확인할 수 있다.
     *
     * @param email 이메일 (null일 시 현재 유저의 이메일 사용)
     * @param question 질문 내용
     * @param callback 콜백
     * @return Task
     */
    public static Task sendQuestion(String email,
                                    String question,
                                    final SaveCallback callback) {

        if (email == null && User.isLogined()) email = User.getCurrentUser().getEmail();

        Param param = new Param();
        param.put("emailaddress", email);
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
                    if (callback != null) callback.done(new HaruException(task.getError()));
                    throw task.getError();

                } else if (task.getResult().hasError()) {
                    Exception e = task.getResult().getError();
                    Haru.stackTrace(e);
                    if (callback != null) callback.done(new HaruException(e));
                    throw e;
                }

                // callback
                if (callback != null) callback.done(null);

                return task.getResult();
            }
        });
    }

    /**
     * 특정 카테고리 안의 FAQ를 가져온다.
     * @param categoryName 카테고리 이름
     * @param callback 콜백
     * @return Task
     */
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

    /**
     * FAQ의 카테고리 목록을 가져온다.
     * @param callback 콜백
     * @return Task
     */
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
                    categories.add(resultsArray.getJSONObject(i).getString("category"));
                }

                // callback
                callback.done(categories, null);

                return categories;
            }
        });
    }
}
