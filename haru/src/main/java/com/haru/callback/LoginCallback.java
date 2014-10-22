package com.haru.callback;

import com.haru.Entity;
import com.haru.HaruException;
import com.haru.User;

public interface LoginCallback {
    public void done(User user, HaruException error);
}
