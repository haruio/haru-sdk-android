package com.haru.callback;

import com.haru.HaruException;
import com.haru.User;

/**
 * 로그인 후, 완료 시 결과를 받기 위한 콜백이다.
 */
public interface LoginCallback {

    /**
     * 로그인 완료 시 호출된다.
     * @param user User 현재 로그인된 유저 {@link com.haru.User}
     * @param error HaruException (정상적인 결과일 시 null)
     */
    public void done(User user, HaruException error);
}
