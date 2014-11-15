package com.haru;

/**
 * Haru Android SDK를 초기화하는 역할이자,
 * 각종 함수들을 모아놓은 Utility Class의 역할을 한다.
 */

public class HaruException extends Exception {

    // 로그인 에러 : 해당 계정이 존재하지 않음
    public static final int NO_SUCH_ACCOUNT = 200;

    // 기타 에러 (서버 내부 오류)
    public static final int OTHER_CAUSE = 999;

    private int code;

    public HaruException(String message) {
        super(message);
    }

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

    /**
     * API 호출 에러시, 에러코드를 반환한다.
     * @return 에러 코드
     */
    public int getErrorCode() {
        return code;
    }

    /**
     * 에러 내용을 반환한다.
     * @return Error Message (String)
     */
    @Override
    public String getMessage() {
        if (getCause() != null) {
            return super.getMessage() + " : " + getCause().getMessage();
        }
        return super.getMessage();
    }
}
