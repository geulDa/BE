package com.CUK.geulDa.global.auth.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    // 로컬 캐시 (단일 서버 환경용)
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String key = null;
        Bucket bucket = null;

        // OAuth2 콜백 경로: IP 기반, 분당 10회
        if (path.startsWith("/api/oauth2/callback/")) {
            String ipAddress = getClientIp(request);
            key = "oauth_callback:" + ipAddress;
            bucket = resolveBucket(key, 10, Duration.ofMinutes(1));
        }
        // 토큰 재발급 경로: IP 기반, 분당 20회
        else if (path.equals("/api/auth/token/refresh")) {
            String ipAddress = getClientIp(request);
            key = "token_refresh:" + ipAddress;
            bucket = resolveBucket(key, 20, Duration.ofMinutes(1));
        }
        // 임시 토큰 교환 경로: IP 기반, 분당 10회
        else if (path.equals("/api/auth/temp-token/exchange")) {
            String ipAddress = getClientIp(request);
            key = "temp_token_exchange:" + ipAddress;
            bucket = resolveBucket(key, 10, Duration.ofMinutes(1));
        }

        // Rate Limit 체크
        if (bucket != null) {
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded: key={}, path={}", key, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"message\":\"요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Bucket 생성 또는 조회
     */
    private Bucket resolveBucket(String key, long capacity, Duration refillDuration) {
        return cache.computeIfAbsent(key, k -> createBucket(capacity, refillDuration));
    }

    /**
     * Bucket 생성
     */
    private Bucket createBucket(long capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillDuration));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * 클라이언트 IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
