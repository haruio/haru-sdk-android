package com.haru.write;

import com.haru.KeyValuePair;

public class UpdateFieldOperation extends Operation<KeyValuePair> {

    public UpdateFieldOperation(String key, String value) {
        super(new KeyValuePair(key, value));
    }

    @Override
    protected String getMethod() {
        return "updateField";
    }
}
