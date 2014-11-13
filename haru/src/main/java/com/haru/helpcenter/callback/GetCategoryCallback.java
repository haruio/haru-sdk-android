package com.haru.helpcenter.callback;

import com.haru.HaruException;
import com.haru.helpcenter.Notice;

import java.util.List;

public interface GetCategoryCallback {
    public void done(List<String> noticeList, HaruException error);
}
