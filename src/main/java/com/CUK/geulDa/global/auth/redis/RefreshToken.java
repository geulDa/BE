package com.CUK.geulDa.global.auth.redis;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.LocalDateTime;

@Getter
@RedisHash("refreshToken")
public class RefreshToken {

    @Id
    private String id; // memberId:deviceId

    private String jti; // 현재 JTI

    private String previousJti; // 이전 JTI (RTR Family ID 추적용)

    private String refreshToken;

    private String deviceName;

    private LocalDateTime lastUsedAt;

    @TimeToLive
    private Long ttl; // 14일 (초 단위)

    @Builder
    public RefreshToken(String id, String jti, String previousJti, String refreshToken,
                       String deviceName, LocalDateTime lastUsedAt, Long ttl) {
        this.id = id;
        this.jti = jti;
        this.previousJti = previousJti;
        this.refreshToken = refreshToken;
        this.deviceName = deviceName;
        this.lastUsedAt = lastUsedAt;
        this.ttl = ttl;
    }

    public void updateToken(String newJti, String newRefreshToken) {
        this.previousJti = this.jti;
        this.jti = newJti;
        this.refreshToken = newRefreshToken;
        this.lastUsedAt = LocalDateTime.now();
    }
}
