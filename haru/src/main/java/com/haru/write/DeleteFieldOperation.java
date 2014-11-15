package com.haru.write;

public class DeleteFieldOperation extends Operation<String> {

    String originalValue;
    public DeleteFieldOperation(String fieldToDeleted) {
        super(fieldToDeleted);
        originalValue = fieldToDeleted;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    @Override
    public String getMethod() {
        return "deleteFields";
    }

    @Override
    public void mergeFromPrevious(Operation other) {
        this.objects.addAll(other.objects);
    }
}
