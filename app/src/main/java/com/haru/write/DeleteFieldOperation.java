package com.haru.write;

public class DeleteFieldOperation extends Operation<String> {

    public DeleteFieldOperation(String fieldToDeleted) {
        super(fieldToDeleted);
    }

    @Override
    protected String getMethod() {
        return "delete";
    }
}
