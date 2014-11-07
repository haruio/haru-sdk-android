package com.haru.helpcenter;

import com.haru.Haru;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * HelpCenter의 QnA 카테고리이다.
 */
public class QuestionCategory {

    private String categoryId;
    private String name;
    private Date createdAt;

    public String getId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    static QuestionCategory fromJson(JSONObject json) {
        try {
            QuestionCategory category   = new QuestionCategory();
            category.categoryId = json.getString("Id");
            category.name       = json.getString("Name");
            category.createdAt  = new Date(json.getLong("Time"));
            return category;

        } catch (JSONException e) {
            Haru.stackTrace(e);
            throw new RuntimeException("Malformed JSON!", e);
        }
    }
}
