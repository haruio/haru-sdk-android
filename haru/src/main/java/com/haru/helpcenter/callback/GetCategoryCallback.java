package com.haru.helpcenter.callback;

import com.haru.HaruException;
import com.haru.helpcenter.Notice;

import java.util.List;

/**
 * FAQ 카테고리 목록을 가져오기 위한 콜백이다.
 * {@link com.haru.helpcenter.HelpCenter#getFaqCategories(GetCategoryCallback)}
 */
public interface GetCategoryCallback {
    /**
     * 서버로부터 응답이 오고 나서 호출된다.
     * @param categoryList 공지사항 목록 (String)
     * @param error HaruException (정상적인 호출일 시 null)
     */
    public void done(List<String> categoryList, HaruException error);
}
