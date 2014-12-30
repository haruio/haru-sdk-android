package com.haru.callback;

import com.haru.Entity;
import com.haru.HaruException;

import java.util.List;

/**
 * 쿼리 완료 후 호출된다. 여러 개의 결과를 받을 때 사용된다.
 * {@link com.haru.Query#findAll(FindCallback)}
 */
public interface FindCallback {
    public void done(List<Entity> findResult, HaruException error);
}
