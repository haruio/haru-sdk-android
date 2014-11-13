package com.haru.push;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.haru.Haru;
import com.haru.HaruRequest;
import com.haru.Installation;
import com.haru.Param;
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
    private Param extras;
    private String title="", message="";
    private String query;

    public static class MessageBuilder {
        private ArrayList<String> channels;
        private Param extras;
        private String message;
        private String query;

        public MessageBuilder() {
            extras = new Param();
        }

        public MessageBuilder setMessage(String message) {
            this.message = message;
            return this;
        }

        public MessageBuilder setQuery(String query) {
            this.query = query;
            return this;
        }

        public MessageBuilder putExtra(String key, Object value) {
            extras.put(key, value);
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
            push.type = Push.TYPE_MESSAGE;
            push.channels = channels;
            push.message = message;
            push.extras = extras;
            push.query = query;
            return push;
        }
    }

    public static class NotificationBuilder {
        private ArrayList<String> channels;
        private Param extras;
        private String title;
        private String message;
        private String query;

        public NotificationBuilder() {
            extras = new Param();
        }

        public NotificationBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public NotificationBuilder setMessage(String message) {
            this.message = message;
            return this;
        }

        public NotificationBuilder setQuery(String query) {
            this.query = query;
            return this;
        }

        public NotificationBuilder putExtra(String key, Object value) {
            extras.put(key, value);
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
            push.type = Push.TYPE_NOTIFICATION;
            push.channels = channels;
            push.message = message;
            push.extras = extras;
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
        Installation currentInstallation = Installation.getCurrentInstallation();
        currentInstallation.addChannels(channels);
        currentInstallation.saveInBackground();
    }


    public static void unsubscribe(String channel) {
    }

    Push(Parcel in) {
        try {
            type = in.readInt();
            in.readStringList(channels);
            extras = Param.fromJson(new JSONObject(in.readString()));
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
        out.writeString(extras.toString());
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
        Param params = new Param();
        if (query != null) params.put("where", query);
        if (channels != null) params.put("channels", new JSONArray(channels));
        if (extras != null) params.put("extras", extras);

        Param data = new Param();
        data.put("message", message);
        if (title != null) data.put("title", title);
        params.put("data", data);

        Task pushTask = new HaruRequest("/push")
                .post(params)
                .executeAsync();

        // TODO: 다으으음!!!
    }
}
