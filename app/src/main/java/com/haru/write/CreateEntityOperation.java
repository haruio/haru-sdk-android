package com.haru.write;

import com.haru.Entity;
import com.haru.Haru;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class CreateEntityOperation extends Operation<Entity> {

    public CreateEntityOperation(Entity object) {
        super(object);
    }

    @Override
    public String getMethod() {
        return "createEntity";
    }
}
