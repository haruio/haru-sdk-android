package com.haru.helpcenter;

import com.haru.Haru;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * HelpCenter의 공지이다.
 */
public class Notice {

    private String noticeId;
    private String title, body, imageUrl;
    private Date createdAt;

    public String getId() {
        return noticeId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    static Notice fromJson(JSONObject json) {
        try {
            Notice notice = new Notice();
            notice.noticeId  = json.getString("_id");
            notice.title     = json.getString("title");
            notice.body      = json.getString("body");
            notice.createdAt = new Date(json.getLong("time"));
            notice.imageUrl  = json.getString("url");
            return notice;

        } catch (JSONException e) {
            Haru.stackTrace(e);
            throw new RuntimeException("Malformed JSON!", e);
        }
    }
}
