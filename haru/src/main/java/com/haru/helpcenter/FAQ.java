package com.haru.helpcenter;

import android.os.Parcel;
import android.os.Parcelable;

import com.haru.Haru;
import com.haru.Param;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class FAQ implements Parcelable {

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
