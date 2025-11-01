package com.CUK.geulDa.global.auth.resolver;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.member.service.MemberService;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import com.CUK.geulDa.global.auth.annotation.CurrentMember;
import lombok.RequiredArgsConstructor;
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
 * @CurrentMember 어노테이션이 붙은 파라미터에 현재 로그인한 사용자 정보를 자동으로 주입하는 Resolver
 * - Long 타입: memberId만 반환
 * - Member 타입: Member 엔티티 전체 반환 (DB 조회)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final MemberService memberService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @CurrentMember 어노테이션이 있고, 타입이 Long 또는 Member인 경우 지원
        return parameter.hasParameterAnnotation(CurrentMember.class) &&
               (Long.class.isAssignableFrom(parameter.getParameterType()) ||
                Member.class.isAssignableFrom(parameter.getParameterType()));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                 NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // @CurrentMember 어노테이션의 required 속성 확인
        CurrentMember currentMember = parameter.getParameterAnnotation(CurrentMember.class);
        boolean required = currentMember != null && currentMember.required();

        // 인증 정보가 없는 경우
        if (authentication == null || !authentication.isAuthenticated()) {
            if (!required) {
                log.debug("인증되지 않은 요청에서 @CurrentMember(required=false) 사용 - null 반환");
                return null;
            }
            log.warn("인증되지 않은 요청에서 @CurrentMember 사용 시도");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // Principal이 Long 타입이 아닌 경우 (익명 사용자 등)
        if (!(authentication.getPrincipal() instanceof Long)) {
            if (!required) {
                log.debug("잘못된 Principal 타입이지만 required=false - null 반환");
                return null;
            }
            log.warn("잘못된 Principal 타입: {}", authentication.getPrincipal().getClass());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "올바르지 않은 인증 정보입니다.");
        }

        Long memberId = (Long) authentication.getPrincipal();

        // Long 타입이면 memberId만 반환
        if (Long.class.isAssignableFrom(parameter.getParameterType())) {
            log.debug("@CurrentMember 주입 (Long): memberId={}", memberId);
            return memberId;
        }

        // Member 타입이면 Member 엔티티 조회 후 반환
        if (Member.class.isAssignableFrom(parameter.getParameterType())) {
            Member member = memberService.getMemberById(memberId);
            log.debug("@CurrentMember 주입 (Member): memberId={}, email={}", memberId, member.getEmail());
            return member;
        }

        // 여기 도달하면 안 됨 (supportsParameter에서 걸러짐)
        throw new IllegalStateException("지원하지 않는 파라미터 타입입니다.");
    }
}
