package com.CUK.geulDa.global.auth.oidc;

import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;

import java.util.Map;

public class OidcUserInfoFactory {

    public static OidcUserInfo getOidcUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoUserInfo(attributes);
            case "google" -> new GoogleUserInfo(attributes);
            default -> throw new BusinessException(
                    ErrorCode.UNSUPPORTED_OAUTH2_PROVIDER,
                    "지원하지 않는 OAuth2 Provider: " + registrationId
            );
        };
    }
}
