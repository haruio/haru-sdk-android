package com.haru.helpcenter;

import android.os.Parcel;
import android.os.Parcelable;

import com.haru.Haru;
import com.haru.Param;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Frequently Asked Questions(자주 묻는 질문)
 */
public class FAQ implements Parcelable {

    private String faqId;
    private String title, body, category;
    private Date createdAt;

    /**
     * FAQ 글의 고유 ID를 반환한다.
     * @return Id
     */
    public String getId() {
        return faqId;
    }

    /**
     * FAQ 글 제목을 반환한다.
     * @return 제목 (String)
     */
    public String getTitle() {
        return title;
    }

    /**
     * FAQ 글 내용을 반환한다.
     * @return 내용 (String)
     */
    public String getBody() {
        return body;
    }

    /**
     * FAQ 글의 카테고리를 반환한다.
     * @return 카테고리 이름 (String)
     */
    public String getCategory() {
        return category;
    }

    /**
     * FAQ 글을 등록한 날짜를 반환한다.
     * @return 날짜 (Date)
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    static FAQ fromJson(JSONObject json) {
        try {
            FAQ faq = new FAQ();
            faq.faqId     = json.getString("_id");
            faq.category  = json.getString("category");
            faq.title     = json.getString("title");
            faq.body      = json.getString("body");
            faq.createdAt = new Date(json.getLong("time"));
            return faq;

        } catch (JSONException e) {
            Haru.stackTrace(e);
            throw new RuntimeException("Malformed JSON!", e);
        }
    }

    public FAQ() {}

    FAQ(Parcel in) {
        faqId = in.readString();
        title = in.readString();
        body  = in.readString();
        category = in.readString();
        createdAt = new Date(in.readLong());
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeString(faqId);
        out.writeString(title);
        out.writeString(body);
        out.writeString(category);
        out.writeLong(createdAt.getTime());
    }


    static final Parcelable.Creator<FAQ> CREATOR = new Parcelable.Creator<FAQ>() {
        @Override
        public FAQ createFromParcel(Parcel in) {
            return new FAQ(in);
        }

        @Override
        public FAQ[] newArray(int size) {
            return new FAQ[size];
        }
    };
}
