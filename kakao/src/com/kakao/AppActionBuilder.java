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

import java.util.HashSet;
import java.util.Set;

import com.kakao.internal.AppActionInfo;
import com.kakao.internal.Action;

/**
 * 카카오링크 톡메시지 중 앱으로 연결할 url을 설정하는 Builder.
 * OS별, 디바이스별 설정이 가능하다.
* @author MJ
*/
public class AppActionBuilder {
    /**
     * 앱으로 연결할 url을 디바이스별로 구분할 때 사용된다.
     */
    public enum DEVICE_TYPE {
        /**
         * 핸드폰
         */
        PHONE("phone"),
        /**
         * 패드
         */
        PAD("pad");

        private final String value;

        DEVICE_TYPE(String value) {
            this.value = value;
        }

        /**
         * 디바이스 종류의 string 값.
         * 메시지를 json으로 만들때 사용된다.
         * @return 디바이스 종류의 string 값. 메시지를 json으로 만들때 사용된다.
         */
        public String getValue() {
            return value;
        }
    }

    private final Set<AppActionInfo> appActionInfos;

    /**
     * 앱링크/앱버튼 사용시 OS별, device type별 설정하기 위해 필요한 빌더를 생성한다.
     */
    public AppActionBuilder() {
        appActionInfos = new HashSet<AppActionInfo>();
    }

    /**
     * {@link AppActionInfoBuilder}를 이용하여 OS별, device type별 AppActionInfo를 생성후 추가한다.
     * @param appActionInfo OS별, device type별 AppActionInfo
     * @return 다른 OS별, device type별 AppActionInfo을 추가할 수 있도록 {@link AppActionInfoBuilder} 반환
     */
    public AppActionBuilder addActionInfo(final AppActionInfo appActionInfo) {
        appActionInfos.add(appActionInfo);
        return this;
    }

    /**
     * 앱 연결 URL 설정이 완료되어 Action에 추가하고 이를 반환한다.
     * @return 앱 연결 URL이 설정된 Action
     * @throws KakaoParameterException 프로토콜에 맞지 않는 설정을 한 경우 던지는 에러
     */
    public Action build() throws KakaoParameterException {
        return Action.newActionApp(appActionInfos.toArray(new AppActionInfo[appActionInfos.size()]));
    }

    /**
     * {@link AppActionInfoBuilder#createAndroidActionInfoBuilder()}.setExecuteParam(String).build() 사용하도록 한다.
     */
    @Deprecated
    public AppActionBuilder setAndroidExecuteURLParam(final String executeURLParam){
        return setAndroidExecuteURLParam(executeURLParam, null);
    }

    /**
     * {@link AppActionInfoBuilder#createAndroidActionInfoBuilder(com.kakao.AppActionBuilder.DEVICE_TYPE)).setExecuteParam(String).build() 사용하도록 한다.
     */
    @Deprecated
    public AppActionBuilder setAndroidExecuteURLParam(final String executeURLParam, final DEVICE_TYPE deviceType){
        AppActionInfo androidAppActionInfo =  AppActionInfo.createAndroidActionInfo(deviceType, executeURLParam, null);
        appActionInfos.add(androidAppActionInfo);
        return this;
    }

    /**
     * {@link AppActionInfoBuilder#createiOSActionInfoBuilder(com.kakao.AppActionBuilder.DEVICE_TYPE)).setExecuteParam(String).build() 사용하도록 한다.
     */
    @Deprecated
    public AppActionBuilder setIOSExecuteURLParam(final String executeURLParam){
        AppActionInfo iosAppActionInfo =  AppActionInfo.createIOSActionInfo(null, executeURLParam, null);
        appActionInfos.add(iosAppActionInfo);
        return this;
    }

    /**
     * {@link AppActionInfoBuilder#createiOSActionInfoBuilder(com.kakao.AppActionBuilder.DEVICE_TYPE)).setExecuteParam(String).build() 사용하도록 한다.
     */
    @Deprecated
    public AppActionBuilder setIOSExecuteURLParam(final String executeURLParam, final DEVICE_TYPE deviceType){
        AppActionInfo iosAppActionInfo =  AppActionInfo.createIOSActionInfo(deviceType, executeURLParam, null);
        appActionInfos.add(iosAppActionInfo);
        return this;
    }
}
