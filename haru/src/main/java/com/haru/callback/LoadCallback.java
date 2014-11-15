package com.haru.callback;

import com.haru.HaruException;

/**
 * 서버로부터 설정을 로딩하고 나서 호출된다.
 */
public interface LoadCallback {
    /**
     * 로딩 완료시 호출된다. 호출된 이후로부터 Config.get을 통해 설정에 접근할 수 있다.
     * @param error 에러 (정상적인 결과일 시, null)
     */
    public void done(HaruException error);
}
