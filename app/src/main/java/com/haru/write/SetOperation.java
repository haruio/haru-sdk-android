package com.haru.write;

import com.haru.KeyValuePair;

import java.util.Iterator;

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
