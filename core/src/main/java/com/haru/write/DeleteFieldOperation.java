package com.haru.write;

import com.haru.Haru;

import java.util.ArrayList;
import java.util.List;

public class DeleteFieldOperation implements Operation {

    private List<String> fields;

    public DeleteFieldOperation(String fieldToDeleted) {
        fields = new ArrayList<String>();
        fields.add(fieldToDeleted);
    }

    public String getFirstValue() {
        return fields.get(0);
    }

    @Override
    public String getMethod() {
        return "deleteFields";
    }

    @Override
    public String getRequestDataKey() {
        return "fields";
    }

    @Override
    public void mergeFromPrevious(Operation other) {
        if (other instanceof DeleteFieldOperation) {
            this.fields.addAll(((DeleteFieldOperation) other).fields);
        }
    }

    @Override
    public Object toJson() throws Exception {
        return Haru.encode(fields);
    }
}
