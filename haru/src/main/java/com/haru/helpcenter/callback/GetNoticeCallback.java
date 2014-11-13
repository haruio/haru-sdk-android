package com.haru.helpcenter.callback;

import com.haru.HaruException;
import com.haru.helpcenter.Notice;

import java.util.ArrayList;
import java.util.List;

public interface GetNoticeCallback {
    public void done(ArrayList<Notice> noticeList, HaruException error);
}
