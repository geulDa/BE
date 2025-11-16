package com.CUK.geulDa.ai.service.session;

import com.CUK.geulDa.ai.dto.CourseRecommendResponse;
import com.CUK.geulDa.ai.dto.SessionData;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationSessionManager {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveSession(String sessionId, Long memberId,
                            List<CourseRecommendResponse.PlaceDetail> places,
                            String travelPurpose, String stayDuration, String transportation) {
        String key = "ai:session:" + sessionId;
        SessionData sessionData = new SessionData(
                memberId,
                places,
                LocalDateTime.now(),
                travelPurpose,
                stayDuration,
                transportation
        );

        redisTemplate.opsForValue().set(key, sessionData, Duration.ofMinutes(30));
        log.debug("세션 저장 완료: sessionId={}, memberId={}, 목적={}, 교통수단={}",
                sessionId, memberId, travelPurpose, transportation);
    }

    public SessionData getSession(String sessionId) {
        String key = "ai:session:" + sessionId;

        try {
            Object session = redisTemplate.opsForValue().get(key);

            if (session == null) {
                throw new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND,
                        "세션 ID: " + sessionId);
            }

            if (!(session instanceof SessionData)) {
                log.error("세션 데이터 타입 불일치: sessionId={}, type={}", sessionId,
                        session.getClass().getName());
                redisTemplate.delete(key);
                throw new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND,
                        "세션이 만료되었거나 손상되었습니다: " + sessionId);
            }

            return (SessionData) session;

        } catch (SerializationException e) {
            log.error("세션 역직렬화 실패: sessionId={}", sessionId, e);
            redisTemplate.delete(key);
            throw new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND,
                    "세션이 만료되었거나 손상되었습니다: " + sessionId);
        }
    }
}
