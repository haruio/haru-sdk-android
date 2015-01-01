package com.haru.helpcenter.callback;

import com.haru.HaruException;
import com.haru.helpcenter.FAQ;

import java.util.List;

/**
 * FAQ를 가져오기 위한 콜백이다.
 * {@link com.haru.helpcenter.HelpCenter#getFrequentlyAskedQuestions(String, GetFAQCallback)}
 */
public interface GetFAQCallback {
    /**
     * 서버로부터 응답이 오고 나서 호출된다.
     * @param faqList FAQ 목록
     * @param error HaruException (정상적인 호출일 시 null)
     */
    public void done(List<FAQ> faqList, HaruException error);
}
