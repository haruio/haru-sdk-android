package com.haru.push;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.haru.Haru;
import com.haru.Installation;
import com.haru.PushService;
import com.haru.Query;
import com.haru.task.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Push implements Parcelable {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_NOTIFICATION = 1;
    public static final String INTENT_EXTRA = "intent.extra.push";

    private int type;
    private ArrayList<String> channels;
    private JSONObject extras;
    private String title="", message="";
    private String query;

    private static Context staticContext;

    public class MessageBuilder {

    }

    public class NotificationBuilder {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Push를 사용한다.
     * @param context Application Context
     */
    public static void init(Context context) {
        staticContext = context;
        PushService.startIfRequired(context);
    }

    /**
     * 특정 채널을 수신한다.
     * @param channel 수신할 채널 이름
     */
    public static void subscribe(String channel) {
        Installation currentInstallation = Installation.getCurrentInstallation();
        currentInstallation.addChannel(channel);
        currentInstallation.saveInBackground();
    }

    /**
     * 특정 채널을 수신한다.
     * @param channels 수신할 채널 목록
     */
    public static void subscribe(List<String> channels) {

    }


    public static void unsubscribe(String channel) {
    }

    Push(Parcel in) {
        try {
            type = in.readInt();
            in.readStringList(channels);
            extras = new JSONObject(in.readString());
            title = in.readString();
            message = in.readString();
            query = in.readString();

        } catch (JSONException e) {
            throw new RuntimeException("Parcel failed : Malformed JSON", e);
        }
    }

    Push(String message) {
        this.message = message;
    }


    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeInt(type);
        out.writeStringList(channels);
        if (extras != null) {
            out.writeString(extras.toString());
        }
        out.writeString(title);
        out.writeString(message);
        out.writeString(query);
    }


    static final Parcelable.Creator<Push> CREATOR = new Parcelable.Creator<Push>() {
        @Override
        public Push createFromParcel(Parcel in) {
            return new Push(in);
        }

        @Override
        public Push[] newArray(int size) {
            return new Push[size];
        }
    };

    /**
     * Push의 종류를 구한다.
     * @return Push Type (Push.TYPE_MESSAGE | Push.TYPE_NOTIFICATION)
     */
    public int getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getQuery() {
        return query;
    }

    public void sendInBackground() {
        try {

            JSONObject params = new JSONObject();
            if (query != null) params.put("where", query);
            if (channels != null) params.put("channels", new JSONArray(channels));
            if (extras != null) params.put("extras", extras);

            JSONObject data = new JSONObject();
            data.put("message", message);
            if (title != null) data.put("title", title);
            params.put("data", data);

            Task pushTask = Haru.newPushRequest("/push")
                    .post(params)
                    .executeAsync();

            // TODO: 다으으음!!!

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
