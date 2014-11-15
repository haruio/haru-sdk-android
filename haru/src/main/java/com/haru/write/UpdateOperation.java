package com.haru.write;

import java.util.Iterator;

public class UpdateOperation extends Operation<KeyValuePair> {

    public UpdateOperation(KeyValuePair keyValuePair) {
        super(keyValuePair);
    }

    public UpdateOperation(String key, Object value) {
        this(new KeyValuePair(key, value));
    }

    @Override
    public String getMethod() {
        return "update";
    }

    @Override
    public void mergeFromPrevious(Operation other) {
        this.objects.addAll(other.objects);
    }

    public void removeOperationByKey(String key) {
        Iterator<KeyValuePair> iter = objects.iterator();
        while (iter.hasNext()) {
            KeyValuePair obj = iter.next();
            if (obj.getKey().equals(key)) {
                iter.remove();
                return;
            }
        }
    }
}
