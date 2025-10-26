package com.CUK.geulDa.global.auth.oidc;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class GoogleUserInfo implements OidcUserInfo {

    private final Map<String, Object> attributes;

    @Override
    public String getProviderId() {
        Object sub = attributes.get("sub");
        return sub != null ? (String) sub : null;
    }

    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        return email != null ? (String) email : null;
    }

    @Override
    public String getName() {
        Object name = attributes.get("name");
        return name != null ? (String) name : null;
    }

    @Override
    public String getProfileImageUrl() {
        Object picture = attributes.get("picture");
        return picture != null ? (String) picture : null;
    }
}
