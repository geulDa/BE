package com.CUK.geulDa.global.auth.oidc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class KakaoUserInfo implements OidcUserInfo {

    private final Map<String, Object> attributes;

    @Override
    public String getProviderId() {
        Object sub = attributes.get("sub");
        if (sub != null) {
            log.debug("Kakao OIDC - sub: {}", sub);
            return String.valueOf(sub);
        }

        Object id = attributes.get("id");
        if (id != null) {
            log.debug("Kakao OAuth2 - id: {}", id);
            return String.valueOf(id);
        }

        log.warn("Kakao - ProviderId 없음. attributes: {}", attributes.keySet());
        return null;
    }

    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        if (email != null) {
            log.debug("Kakao OIDC - email: {}", email);
            return (String) email;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount != null) {
            Object accountEmail = kakaoAccount.get("email");
            if (accountEmail != null) {
                log.debug("Kakao OAuth2 - kakao_account.email: {}", accountEmail);
                return (String) accountEmail;
            }
        }

        log.warn("Kakao - Email 없음");
        return null;
    }

    @Override
    public String getName() {
        Object nickname = attributes.get("nickname");
        if (nickname != null) {
            log.debug("Kakao OIDC - nickname: {}", nickname);
            return (String) nickname;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        if (properties != null) {
            Object propertiesNickname = properties.get("nickname");
            if (propertiesNickname != null) {
                log.debug("Kakao OAuth2 - properties.nickname: {}", propertiesNickname);
                return (String) propertiesNickname;
            }
        }

        log.warn("Kakao - Nickname 없음");
        return "Unknown";
    }

    @Override
    public String getProfileImageUrl() {
        Object picture = attributes.get("picture");
        if (picture != null) {
            log.debug("Kakao OIDC - picture: {}", picture);
            return (String) picture;
        }

        Object profileImageUrl = attributes.get("profile_image_url");
        if (profileImageUrl != null) {
            log.debug("Kakao OIDC - profile_image_url: {}", profileImageUrl);
            return (String) profileImageUrl;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        if (properties != null) {
            Object profileImage = properties.get("profile_image");
            if (profileImage != null) {
                log.debug("Kakao OAuth2 - properties.profile_image: {}", profileImage);
                return (String) profileImage;
            }
        }

        log.debug("Kakao - ProfileImage 없음 (선택 사항)");
        return null;
    }
}
