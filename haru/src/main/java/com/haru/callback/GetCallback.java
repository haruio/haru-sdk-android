package com.haru.callback;

import com.haru.Entity;
import com.haru.HaruException;

/**
 * 엔티티를 서버로부터 가져올 때 사용된다.
 * {@link com.haru.Entity}
 */
public interface GetCallback {
    /**
     * 서버에서 Entity를 가져오고 나서 호출된다.
     * @param entity 가져온 Entity
     * @param error HaruException (정상적인 결과일시 null)
     */
    public void done(Entity entity, HaruException error);
}
