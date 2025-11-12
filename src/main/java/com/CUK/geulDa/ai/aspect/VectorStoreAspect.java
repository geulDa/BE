package com.CUK.geulDa.ai.aspect;

import com.CUK.geulDa.ai.service.ChatbotService;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 벡터스토어 준비 상태를 체크하는 AOP
 * - @RequireVectorStore 어노테이션이 붙은 메서드 실행 전에 상태 검증
 * - 준비되지 않았으면 503 Service Unavailable 응답
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorStoreAspect {

    private final ChatbotService chatbotService;

    @Before("@annotation(RequireVectorStore)")
    public void checkVectorStoreReady() {
        if (!chatbotService.isVectorStoreReady()) {
            log.warn("⚠️ 벡터스토어 미준비 상태에서 API 호출 시도");
            throw new BusinessException(
                ErrorCode.AI_SERVICE_ERROR,
                "벡터 스토어가 초기화 중입니다. 잠시 후 다시 시도해주세요."
            );
        }
    }
}
