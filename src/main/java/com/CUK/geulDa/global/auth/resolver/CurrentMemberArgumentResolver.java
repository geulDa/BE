package com.CUK.geulDa.global.auth.resolver;

import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import com.CUK.geulDa.global.auth.annotation.CurrentMember;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @CurrentMember 어노테이션이 붙은 파라미터에 현재 로그인한 사용자의 ID를 자동으로 주입하는 Resolver
 */
@Component
@Slf4j
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @CurrentMember 어노테이션이 있고, 타입이 Long인 경우에만 지원
        return parameter.hasParameterAnnotation(CurrentMember.class) &&
               Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                 NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없는 경우
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("인증되지 않은 요청에서 @CurrentMember 사용 시도");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // Principal이 Long 타입이 아닌 경우 (익명 사용자 등)
        if (!(authentication.getPrincipal() instanceof Long)) {
            log.warn("잘못된 Principal 타입: {}", authentication.getPrincipal().getClass());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "올바르지 않은 인증 정보입니다.");
        }

        Long memberId = (Long) authentication.getPrincipal();
        log.debug("@CurrentMember 주입: memberId={}", memberId);
        return memberId;
    }
}
