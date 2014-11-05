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

    public String getNoticeId() {
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
            notice.noticeId  = json.getString("Id");
            notice.title     = json.getString("Title");
            notice.body      = json.getString("Body");
            notice.createdAt = new Date(json.getLong("Time"));
            notice.imageUrl  = json.getString("URL");
            return notice;

        } catch (JSONException e) {
            Haru.stackTrace(e);
            throw new RuntimeException("Malformed JSON!", e);
        }
    }
}
