package com.haru.write;

import com.haru.KeyValuePair;

import java.util.ArrayList;

public class CreateFieldOperation extends Operation<KeyValuePair> {

    public CreateFieldOperation(KeyValuePair keyValuePair) {
        super(keyValuePair);
    }

    public CreateFieldOperation(String key, String value) {
        this(new KeyValuePair(key, value));
    }

    @Override
    public String getMethod() {
        return "createField";
    }
}
