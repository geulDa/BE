package com.CUK.geulDa.global.auth.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class BlacklistRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * JTI를 Blacklist에 추가
     */
    public void add(String jti, long ttlMillis) {
        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "REVOKED", ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * JTI가 Blacklist에 있는지 확인
     */
    public boolean exists(String jti) {
        String key = BLACKLIST_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
