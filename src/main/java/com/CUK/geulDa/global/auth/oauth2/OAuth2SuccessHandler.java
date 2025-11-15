package com.CUK.geulDa.global.auth.oauth2;

import com.CUK.geulDa.global.auth.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;

    private static final String FRONTEND_URL = "https://www.geulda.kr";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException {
        log.info("=== OAuth2 인증 성공 ===");

        // Principal 타입 확인
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomOidcUser)) {
            log.error("지원하지 않는 Principal 타입: {}", principal.getClass().getName());
            log.error("Principal 내용: {}", principal);
            throw new IllegalStateException(
                "OAuth2 인증에 실패했습니다. OIDC가 활성화되어 있는지 확인해주세요."
            );
        }

        // OIDC 사용자 정보 추출 (Google & Kakao 모두)
        CustomOidcUser oidcUser = (CustomOidcUser) principal;
        Long memberId = oidcUser.getMemberId();

        log.info("OIDC 인증 완료 - memberId: {}", memberId);

        // User-Agent에서 디바이스 정보 추출
        String userAgent = request.getHeader("User-Agent");
        String deviceName = extractDeviceName(userAgent);

        // 디바이스 ID 생성 (User-Agent 기반 해시 또는 UUID)
        // 방법 1: User-Agent 해시 (같은 브라우저면 같은 deviceId)
        // String deviceId = userAgent != null ? String.valueOf(userAgent.hashCode()) : "unknown";

        // 방법 2: 매번 새로운 UUID 생성 (더 안전하지만 디바이스 추적 불가)
        String deviceId = UUID.randomUUID().toString();

        log.info("디바이스 정보 - deviceId: {}, deviceName: {}", deviceId, deviceName);

        // 임시 토큰 생성 (Redis에 저장, TTL: 600초)
        String tempToken = tokenService.createTempToken(memberId, deviceId, deviceName);

        log.info("임시 토큰 생성 완료 - tempToken: {}", tempToken);

        // 프론트엔드로 리다이렉트 (tempToken 쿼리 파라미터로 전달)
        String redirectUrl = UriComponentsBuilder.fromUriString(FRONTEND_URL)
            .path("/auth/callback")
            .queryParam("temp_token", tempToken)
            .build()
            .toUriString();

        log.info("프론트엔드로 리다이렉트 - URL: {}", redirectUrl);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * User-Agent에서 디바이스 이름 추출
     * @param userAgent HTTP User-Agent 헤더 값
     * @return 디바이스 이름 (iPhone, Android, Mac, Windows 등)
     */
    private String extractDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown Device";
        }

        // 모바일 디바이스 우선 검사
        if (userAgent.contains("iPhone")) return "iPhone";
        if (userAgent.contains("iPad")) return "iPad";
        if (userAgent.contains("Android")) {
            // Android 상세 정보 추출 시도
            if (userAgent.contains("Mobile")) return "Android Mobile";
            if (userAgent.contains("Tablet")) return "Android Tablet";
            return "Android";
        }

        // 데스크톱 OS 검사
        if (userAgent.contains("Macintosh") || userAgent.contains("Mac OS")) return "Mac";
        if (userAgent.contains("Windows NT 10.0")) return "Windows 10/11";
        if (userAgent.contains("Windows NT 6.3")) return "Windows 8.1";
        if (userAgent.contains("Windows NT 6.2")) return "Windows 8";
        if (userAgent.contains("Windows NT 6.1")) return "Windows 7";
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Linux")) return "Linux";

        // 브라우저 기반 추출
        if (userAgent.contains("Chrome")) return "Chrome Browser";
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) return "Safari Browser";
        if (userAgent.contains("Firefox")) return "Firefox Browser";
        if (userAgent.contains("Edge")) return "Edge Browser";

        // 기본값
        return "Web Browser";
    }
}
