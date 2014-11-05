package com.kakao;

/**
 * @author MJ
 */
public enum AuthType {
    KAKAO_TALK(0),
    KAKAO_STORY(1),
    KAKAO_ACCOUNT(2);

    private final int number;

    AuthType(int i) {
        this.number = i;
    }

    public int getNumber() {
        return number;
    }

    public static AuthType valueOf(int number){
        if(number == KAKAO_TALK.getNumber()) {
            return KAKAO_TALK;
        } else if (number == KAKAO_STORY.getNumber()) {
            return KAKAO_STORY;
        } else if (number == KAKAO_ACCOUNT.getNumber()) {
            return KAKAO_ACCOUNT;
        } else {
            return null;
        }
    }
}
