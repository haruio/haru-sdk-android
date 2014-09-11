package com.haru;

public class HaruException extends Exception {
    private int code;

    public HaruException(int code, String message) {
        super(message);
        this.code = code;
    }

    public HaruException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
    }

    public HaruException(Throwable cause) {
        super(cause);
        this.code = -1;
    }
}
