package com.haru.helpcenter;

import com.haru.Haru;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class FAQ {

    private String faqId;
    private String title, body, category;
    private Date createdAt;

    public String getId() {
        return faqId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getCategory() {
        return category;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    static FAQ fromJson(JSONObject json) {
        try {
            FAQ faq = new FAQ();
            faq.faqId     = json.getString("Id");
            faq.category  = json.getString("Category");
            faq.title     = json.getString("Title");
            faq.body      = json.getString("Body");
            faq.createdAt = new Date(json.getLong("Time"));
            return faq;

        } catch (JSONException e) {
            Haru.stackTrace(e);
            throw new RuntimeException("Malformed JSON!", e);
        }
    }
}
