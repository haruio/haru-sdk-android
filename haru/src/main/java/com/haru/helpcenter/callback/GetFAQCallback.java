package com.haru.helpcenter.callback;

import com.haru.HaruException;
import com.haru.helpcenter.FAQ;
import com.haru.helpcenter.Notice;

import java.util.List;

public interface GetFAQCallback {
    public void done(List<FAQ> faqList, HaruException error);
}
