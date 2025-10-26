package com.CUK.geulDa.global.auth.oidc;

public interface OidcUserInfo {

    String getProviderId();

    String getEmail();

    String getName();

    String getProfileImageUrl();
}
