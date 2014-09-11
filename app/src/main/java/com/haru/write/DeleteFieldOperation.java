package com.haru.write;

import com.haru.KeyValuePair;

public class DeleteFieldOperation extends Operation<KeyValuePair> {

    public DeleteFieldOperation(KeyValuePair object) {
        super(object);
    }

    @Override
    protected String getMethod() {
        return "delete";
    }
}
