package com.haru.callback;

import com.haru.Entity;
import com.haru.HaruException;

public interface GetCallback {
    public void done(Entity entity, HaruException error);
}
