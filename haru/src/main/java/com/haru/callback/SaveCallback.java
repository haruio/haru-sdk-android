package com.haru.callback;

import com.haru.HaruException;

/**
 * 서버에 데이터를 저장하기 위한 콜백이다.
 */
public interface SaveCallback {
    /**
     * 데이터 저장이 완료되고 나서 호출된다.
     * @param error HaruException (정상적인 결과였을 시 null)
     */
    public void done(HaruException error);
}
