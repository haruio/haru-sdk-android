package com.haru.callback;

import com.haru.Entity;
import com.haru.HaruException;

import java.util.List;

public interface FindCallback {
    public void done(List<Entity> findResult, HaruException error);
}
