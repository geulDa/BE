package com.CUK.geulDa.global.auth.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    // 프론트엔드 URL (개발/프로덕션 환경에 따라 변경)
    private static final String FRONTEND_URL = "https://www.geulda.kr";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {
        String errorMessage = exception.getMessage();
        log.error("OAuth2 인증 실패: {}", errorMessage, exception);

        // 프론트엔드 에러 페이지로 리다이렉트
        String redirectUrl = UriComponentsBuilder.fromUriString(FRONTEND_URL)
                .path("/auth/error")
                .queryParam("error", errorMessage)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
