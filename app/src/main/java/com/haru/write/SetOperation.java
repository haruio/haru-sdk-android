package com.haru.write;

import com.haru.KeyValuePair;

public class SetOperation extends Operation<KeyValuePair> {

    public SetOperation(KeyValuePair keyValuePair) {
        super(keyValuePair);
    }

    public SetOperation(String key, Object value) {
        this(new KeyValuePair(key, value));
    }

    @Override
    public String getMethod() {
        return "set";
    }
}
