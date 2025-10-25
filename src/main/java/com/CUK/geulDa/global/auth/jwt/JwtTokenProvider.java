package com.CUK.geulDa.global.auth.jwt;

import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    protected void init() {
        this.secretKey = Keys.hmacShaKeyFor(
            jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(Long memberId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(memberId.toString())
                .id(UUID.randomUUID().toString()) // jti
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(Long memberId, String deviceId, String jti) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(memberId.toString())
                .claim("deviceId", deviceId)
                .id(jti) // 패밀리 ID 추적용
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 토큰 검증 및 Claims 추출
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("만료된 토큰: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, null, e);
        } catch (UnsupportedJwtException e) {
            log.debug("지원하지 않는 토큰: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "지원하지 않는 토큰입니다.", e);
        } catch (MalformedJwtException e) {
            log.debug("잘못된 형식의 토큰: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "잘못된 형식의 토큰입니다.", e);
        } catch (SecurityException e) {
            log.debug("잘못된 서명의 토큰: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "잘못된 서명의 토큰입니다.", e);
        } catch (IllegalArgumentException e) {
            log.debug("유효하지 않은 토큰: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN, null, e);
        }
    }

    /**
     * Member ID 추출
     */
    public Long getMemberId(String token) {
        Claims claims = validateAndGetClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * JTI (JWT ID) 추출
     */
    public String getJti(String token) {
        Claims claims = validateAndGetClaims(token);
        return claims.getId();
    }

    /**
     * Device ID 추출
     */
    public String getDeviceId(String token) {
        Claims claims = validateAndGetClaims(token);
        return claims.get("deviceId", String.class);
    }

    /**
     * 토큰의 남은 만료 시간 (밀리초)
     */
    public long getRemainingExpiration(String token) {
        Claims claims = validateAndGetClaims(token);
        Date expiration = claims.getExpiration();
        long now = System.currentTimeMillis();
        return Math.max(0, expiration.getTime() - now);
    }
}
