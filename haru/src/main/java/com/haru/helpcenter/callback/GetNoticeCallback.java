package com.haru.helpcenter.callback;

import com.haru.HaruException;
import com.haru.helpcenter.Notice;

import java.util.List;

public interface GetNoticeCallback {
    public void done(List<Notice> noticeList, HaruException error);
}
