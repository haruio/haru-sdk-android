package com.haru.push;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.haru.HaruRequest;
import com.haru.Installation;
import com.haru.Param;
import com.haru.Query;
import com.haru.task.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 서버로부터 전송된 Push 데이터를 표현하는 클래스이자, <br/>
 * 서버로 푸시를 보내기 위한 유틸리티 클래스이다.
 */
public class Push implements Parcelable {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_NOTIFICATION = 1;
    public static final String INTENT_EXTRA = "intent.extra.push";

    private ArrayList<String> channels;
    private Param data;
    private String query;

    public static class MessageBuilder {
        private ArrayList<String> channels;
        private Param data;
        private String message;
        private String query;

        public MessageBuilder() {
            data = new Param();
            channels = new ArrayList<String>();

            data.put("type", Push.TYPE_MESSAGE);
        }

        public MessageBuilder setMessage(String message) {
            data.put("message", message);
            return this;
        }

        public MessageBuilder setQuery(String query) {
            this.query = query;
            return this;
        }

        public MessageBuilder putExtra(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public MessageBuilder setChannels(ArrayList<String> channels) {
            this.channels = channels;
            return this;
        }

        public MessageBuilder setChannel(String channel) {
            this.channels = new ArrayList<String>();
            channels.add(channel);
            return this;
        }

        public Push build() {
            Push push = new Push();
            push.channels = channels;
            push.data = data;
            push.query = query;
            return push;
        }
    }

    public static class NotificationBuilder {
        private ArrayList<String> channels;
        private Param data;
        private String query;

        public NotificationBuilder() {
            data = new Param();
            channels = new ArrayList<String>();

            data.put("type", Push.TYPE_NOTIFICATION);
        }

        public NotificationBuilder setTitle(String title) {
            data.put("title", title);
            return this;
        }

        public NotificationBuilder setMessage(String message) {
            data.put("message", message);
            return this;
        }

        public NotificationBuilder setQuery(String query) {
            this.query = query;
            return this;
        }

        public NotificationBuilder putExtra(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public NotificationBuilder setChannels(ArrayList<String> channels) {
            this.channels = channels;
            return this;
        }

        public NotificationBuilder setChannel(String channel) {
            this.channels = new ArrayList<String>();
            channels.add(channel);
            return this;
        }

        public Push build() {
            Push push = new Push();
            push.channels = channels;
            push.data = data;
            push.query = query;
            return push;
        }
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
        PushService.startIfRequired(context);
    }

    private Push() {
        data = new Param();
        channels = new ArrayList<String>();
    }

    /**
     * 특정 채널을 수신한다.
     * @param channel 수신할 채널 이름
     */
    public static void subscribe(String channel) {
        Installation currentInstallation = Installation.getCurrentInstallation();
        currentInstallation.addPushChannel(channel);
        currentInstallation.saveInBackground();
    }

    /**
     * 특정 채널을 수신한다.
     * @param channels 수신할 채널 목록
     */
    public static void subscribe(List<String> channels) {
        Installation currentInstallation = Installation.getCurrentInstallation();
        currentInstallation.addPushChannels(channels);
        currentInstallation.saveInBackground();
    }


    public static void unsubscribe(String channel) {
    }

    Push(Parcel in) {
        channels = new ArrayList<String>();
        try {
            in.readStringList(channels);
            data = Param.fromJson(new JSONObject(in.readString()));
            query = in.readString();

        } catch (JSONException e) {
            throw new RuntimeException("Parcel failed : Malformed JSON", e);
        }
    }

    /**
     * JSON Packet으로 Push를 생성한다.
     */
    static Push fromPacket(String jsonPacket) {
        try {
            Push push = new Push();
            push.data = Param.fromJson(new JSONObject(jsonPacket));
            return push;

        } catch (JSONException e) {
            throw new RuntimeException("Malformed JSON Push Packet!", e);
        }
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeStringList(channels);
        out.writeString(data.toJson().toString());
        out.writeString(query);
    }


    public static final Parcelable.Creator<Push> CREATOR = new Parcelable.Creator<Push>() {
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
        return (Integer) data.get("type");
    }

    public String getTitle() {
        return (String) data.get("title");
    }

    public String getMessage() {
        return (String) data.get("message");
    }

    public String getQuery() {
        return query;
    }

    public void sendInBackground() {
        Param params = new Param();
        if (query != null) params.put("users", query); // TODO: Fix
        if (channels != null) {
            params.put("installations", Query.where("Installations")
                    .containedIn("channels", channels)
                    .getJson());
        }

        params.put("notification", data);

        Task pushTask = new HaruRequest("/push")
                .post(params)
                .executeAsync();

        // TODO: 다으으음!!!
    }
}
