package com.haru.write;

import com.haru.JsonEncodable;

public interface Operation extends JsonEncodable {

    public abstract String getMethod();

    public abstract String getRequestDataKey();

    public abstract void mergeFromPrevious(Operation other);
}
