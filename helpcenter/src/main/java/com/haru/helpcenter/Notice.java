package com.haru.helpcenter;

import com.haru.Haru;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * 고객센터의 공지사항 게시글
 */
public class Notice {

    private String noticeId;
    private String title, body, imageUrl;
    private Date createdAt;

    public String getId() {
        return noticeId;
    }

    /**
     * 공지 제목을 반환한다.
     * @return 제목 (String)
     */
    public String getTitle() {
        return title;
    }

    /**
     * 공지 내용을 반환한다.
     * @return 내용 (String)
     */
    public String getBody() {
        return body;
    }

    /**
     * 공지에 이미지가 있는 경우 해당 이미지의 URL을 반환한다.
     * @return 이미지 URL (없을 시, null 반환)
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * 공지 날짜를 반환한다.
     * @return 공지 날짜 (Date)
     */
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
