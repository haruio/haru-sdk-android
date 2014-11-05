/**
 * Copyright 2014 Kakao Corp.
 *
 * Redistribution and modification in source or binary forms are not permitted without specific prior written permission. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 등록된 토큰 요청에 대한 결과 객체로 토큰 정보가 담겨 있다.
 *
 * @author MJ
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PushTokenInfo {
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("device_id")
    private String deviceId;
    @JsonProperty("push_type")
    private String pushType;
    @JsonProperty("push_token")
    private String pushToken;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;

    /**
     * 사용자의 고유 ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 기기의 고유한 ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * apns 혹은 gcm
     */
    public String getPushType() {
        return pushType;
    }

    /**
     * APNS, GCM으로부터 발급받은 Push Token
     */
    public String getPushToken() {
        return pushToken;
    }

    /**
     * 푸시 토큰을 처음 등록한 시각
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * 푸시 토큰을 업데이트한 시각
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PushTokenInfo{");
        sb.append("userId='").append(userId).append('\'');
        sb.append(", deviceId='").append(deviceId).append('\'');
        sb.append(", pushType='").append(pushType).append('\'');
        sb.append(", pushToken='").append(pushToken).append('\'');
        sb.append(", createdAt='").append(createdAt).append('\'');
        sb.append(", updatedAt='").append(updatedAt).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
