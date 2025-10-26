package com.CUK.geulDa.global.auth.redis;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@RedisHash("tempToken")
public class TempToken {

    @Id
    private String tempToken; // UUID

    private Long memberId;

    private String accessToken;

    private String refreshToken;

    private String deviceId;

    @TimeToLive
    private Long ttl; // 3분 (초 단위)

    @Builder
    public TempToken(String tempToken, Long memberId, String accessToken,
                    String refreshToken, String deviceId, Long ttl) {
        this.tempToken = tempToken;
        this.memberId = memberId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.deviceId = deviceId;
        this.ttl = ttl;
    }
}
