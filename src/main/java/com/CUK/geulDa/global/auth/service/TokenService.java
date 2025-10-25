package com.CUK.geulDa.global.auth.service;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.member.service.MemberService;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import com.CUK.geulDa.global.auth.dto.TokenResponse;
import com.CUK.geulDa.global.auth.jwt.JwtProperties;
import com.CUK.geulDa.global.auth.jwt.JwtTokenProvider;
import com.CUK.geulDa.global.auth.redis.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TempTokenRepository tempTokenRepository;
    private final BlacklistRepository blacklistRepository;
    private final MemberService memberService;
    private final RedisTemplate<String, Object> redisTemplate;

    // 사용자당 최대 디바이스 개수
    private static final int MAX_DEVICES_PER_USER = 5;

    /**
     * 새로운 토큰 발급
     */
    public TokenResponse issueTokens(Long memberId, String deviceId, String deviceName) {
        // 기존 토큰 정리 (최대 디바이스 개수 초과 시)
        cleanupOldRefreshTokens(memberId);

        // JWT 생성
        String jti = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.createAccessToken(memberId);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId, deviceId, jti);

        // Redis에 RefreshToken 저장
        String tokenId = memberId + ":" + deviceId;
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(tokenId)
                .jti(jti)
                .previousJti(null)
                .refreshToken(refreshToken)
                .deviceName(deviceName)
                .lastUsedAt(LocalDateTime.now())
                .ttl(jwtProperties.getRefreshTokenExpiration() / 1000)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("새 토큰 발급: memberId={}, deviceId={}, jti={}", memberId, deviceId, jti);

        return TokenResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiration()
        );
    }

    /**
     * 오래된 Refresh Token 정리
     * 사용자당 최대 MAX_DEVICES_PER_USER개까지만 유지
     */
    private void cleanupOldRefreshTokens(Long memberId) {
        try {
            // Redis에서 해당 사용자의 모든 Refresh Token 조회
            String pattern = "refreshToken:" + memberId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys == null || keys.size() <= MAX_DEVICES_PER_USER) {
                return; // 정리 불필요
            }

            // 각 토큰의 마지막 사용 시간 조회
            List<RefreshTokenInfo> tokens = new ArrayList<>();
            for (String key : keys) {
                String tokenId = key.replace("refreshToken:", "");
                refreshTokenRepository.findById(tokenId).ifPresent(token -> {
                    tokens.add(new RefreshTokenInfo(tokenId, token.getLastUsedAt()));
                });
            }

            // 마지막 사용 시간 기준 오름차순 정렬 (오래된 것부터)
            tokens.sort(Comparator.comparing(RefreshTokenInfo::lastUsedAt));

            // 오래된 토큰 삭제 (최대 개수 초과분)
            int deleteCount = tokens.size() - MAX_DEVICES_PER_USER;
            for (int i = 0; i < deleteCount; i++) {
                String tokenIdToDelete = tokens.get(i).tokenId();
                refreshTokenRepository.deleteById(tokenIdToDelete);
                log.info("오래된 Refresh Token 삭제: memberId={}, tokenId={}", memberId, tokenIdToDelete);
            }

        } catch (Exception e) {
            log.error("Refresh Token 정리 중 오류 발생: memberId={}", memberId, e);
            // 정리 실패해도 토큰 발급은 계속 진행
        }
    }

    /**
     * Refresh Token 정보 (정렬용)
     */
    private record RefreshTokenInfo(String tokenId, LocalDateTime lastUsedAt) {}

    /**
     * Refresh Token Rotation (RTR) - Family ID 추적
     */
    public TokenResponse rotateRefreshToken(String refreshToken) {
        // 토큰 검증
        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        String deviceId = jwtTokenProvider.getDeviceId(refreshToken);
        String currentJti = jwtTokenProvider.getJti(refreshToken);

        // Redis에서 저장된 RefreshToken 조회
        String tokenId = memberId + ":" + deviceId;
        RefreshToken storedToken = refreshTokenRepository.findById(tokenId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        // JTI 체인 검증 (RTR Family ID)
        boolean isValidJti = currentJti.equals(storedToken.getJti()) ||
                            currentJti.equals(storedToken.getPreviousJti());

        if (!isValidJti) {
            // Refresh Token 재사용 감지 - 해당 기기의 모든 토큰 무효화
            log.warn("Refresh Token 재사용 감지! memberId={}, deviceId={}, jti={}",
                    memberId, deviceId, currentJti);
            refreshTokenRepository.deleteById(tokenId);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        // previousJti와 일치하는 경우 (네트워크 지연 등)
        if (currentJti.equals(storedToken.getPreviousJti())) {
            log.info("이전 Refresh Token 사용 (네트워크 지연): memberId={}, deviceId={}",
                    memberId, deviceId);
            // 기존 토큰 그대로 반환
            String accessToken = jwtTokenProvider.createAccessToken(memberId);
            return TokenResponse.of(
                    accessToken,
                    storedToken.getRefreshToken(),
                    jwtProperties.getAccessTokenExpiration()
            );
        }

        // 새 토큰 발급
        String newJti = UUID.randomUUID().toString();
        String newAccessToken = jwtTokenProvider.createAccessToken(memberId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId, deviceId, newJti);

        // RefreshToken 업데이트 (previousJti에 현재 jti 저장)
        storedToken.updateToken(newJti, newRefreshToken);
        refreshTokenRepository.save(storedToken);

        log.info("Refresh Token 갱신: memberId={}, deviceId={}, oldJti={}, newJti={}",
                memberId, deviceId, currentJti, newJti);

        return TokenResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtProperties.getAccessTokenExpiration()
        );
    }

    /**
     * 임시 토큰 생성 (OAuth2 인증 완료 후 프론트로 전달)
     */
    public String createTempToken(Long memberId, String deviceId, String deviceName) {
        // 토큰 발급
        TokenResponse tokenResponse = issueTokens(memberId, deviceId, deviceName);

        // UUID 생성
        String tempTokenId = UUID.randomUUID().toString();

        // Redis에 저장 (3분 TTL)
        TempToken tempToken = TempToken.builder()
                .tempToken(tempTokenId)
                .memberId(memberId)
                .accessToken(tokenResponse.accessToken())
                .refreshToken(tokenResponse.refreshToken())
                .deviceId(deviceId)
                .ttl(180L) // 3분 (초 단위)
                .build();
        tempTokenRepository.save(tempToken);

        log.info("임시 토큰 생성: memberId={}, tempToken={}", memberId, tempTokenId);

        return tempTokenId;
    }

    /**
     * 임시 토큰 교환 (일회성)
     */
    public TokenResponse exchangeTempToken(String tempTokenId) {
        // Redis에서 조회
        TempToken tempToken = tempTokenRepository.findById(tempTokenId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TEMP_TOKEN));

        // 즉시 삭제 (일회성)
        tempTokenRepository.deleteById(tempTokenId);

        log.info("임시 토큰 교환: memberId={}, tempToken={}", tempToken.getMemberId(), tempTokenId);

        return TokenResponse.of(
                tempToken.getAccessToken(),
                tempToken.getRefreshToken(),
                jwtProperties.getAccessTokenExpiration()
        );
    }

    /**
     * 로그아웃
     */
    public void logout(String authorization, String refreshToken) {
        // Authorization 헤더 검증
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Authorization 헤더가 올바르지 않습니다.");
        }

        // Bearer 제거하여 Access Token 추출
        String accessToken = authorization.substring(7);

        // Access Token이 비어있는지 검증
        if (accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Access Token이 비어있습니다.");
        }

        // Access Token의 JTI를 Blacklist에 추가
        String accessJti = jwtTokenProvider.getJti(accessToken);
        long remainingExpiration = jwtTokenProvider.getRemainingExpiration(accessToken);
        blacklistRepository.add(accessJti, remainingExpiration);

        // Refresh Token 삭제
        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        String deviceId = jwtTokenProvider.getDeviceId(refreshToken);
        String tokenId = memberId + ":" + deviceId;
        refreshTokenRepository.deleteById(tokenId);

        log.info("로그아웃: memberId={}, deviceId={}", memberId, deviceId);
    }
}
