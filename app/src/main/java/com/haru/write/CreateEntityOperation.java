package com.haru.write;

import com.haru.Entity;

public class CreateEntityOperation extends Operation<Entity> {

    public CreateEntityOperation(Entity object) {
        super(object);
    }

    @Override
    public String getMethod() {
        return "createEntity";
    }

    @Override
    public void mergeFromPrevious(Operation other) {
        this.objects.addAll(other.objects);
    }
}
