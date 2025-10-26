package com.CUK.geulDa.global.auth.oauth2;

import com.CUK.geulDa.domain.member.constant.Provider;
import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.member.service.MemberService;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import com.CUK.geulDa.global.auth.oidc.OidcUserInfo;
import com.CUK.geulDa.global.auth.oidc.OidcUserInfoFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final MemberService memberService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String NONCE_PREFIX = "nonce:";
    private static final long NONCE_TTL = 10; // 10분

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 부모 클래스의 loadUser 호출
        OidcUser oidcUser = super.loadUser(userRequest);

        // Nonce 검증 (중복 요청 방지)
        String nonce = oidcUser.getIdToken().getClaim("nonce");
        if (nonce != null) {
            String nonceKey = NONCE_PREFIX + nonce;
            Boolean exists = redisTemplate.hasKey(nonceKey);
            if (Boolean.TRUE.equals(exists)) {
                log.warn("중복된 nonce 감지: {}", nonce);
                throw new OAuth2AuthenticationException("중복된 인증 요청입니다.");
            }
            // Nonce를 Redis에 저장 (TTL 10분)
            redisTemplate.opsForValue().set(nonceKey, "used", NONCE_TTL, TimeUnit.MINUTES);
        }

        try {
            // Provider 정보 추출
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            Provider provider = Provider.valueOf(registrationId.toUpperCase());

            // OIDC 사용자 정보 변환
            OidcUserInfo userInfo = OidcUserInfoFactory.getOidcUserInfo(registrationId, oidcUser.getAttributes());

            // 필수 정보 검증
            validateUserInfo(userInfo, provider);

            // 회원 조회 또는 생성
            Member member = memberService.getOrCreateMember(userInfo, provider);

            // CustomOidcUser 반환 (memberId 포함)
            return new CustomOidcUser(oidcUser, member.getId());

        } catch (BusinessException e) {
            // BusinessException은 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("OAuth2 사용자 처리 중 오류 발생", e);
            throw new BusinessException(ErrorCode.OAUTH2_AUTHENTICATION_FAILED, "사용자 정보 처리에 실패했습니다.", e);
        }
    }

    /**
     * OAuth2 사용자 정보 필수 필드 검증
     */
    private void validateUserInfo(OidcUserInfo userInfo, Provider provider) {
        // 이메일 필수 검증
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            log.warn("OAuth2 사용자 정보에 이메일이 없음: provider={}", provider);
            throw new BusinessException(
                ErrorCode.OAUTH2_AUTHENTICATION_FAILED,
                "이메일 정보가 필요합니다. " + provider.name() + " 로그인 시 이메일 제공에 동의해주세요."
            );
        }

        // 이름 필수 검증
        if (userInfo.getName() == null || userInfo.getName().isBlank()) {
            log.warn("OAuth2 사용자 정보에 이름이 없음: provider={}", provider);
            throw new BusinessException(
                ErrorCode.OAUTH2_AUTHENTICATION_FAILED,
                "이름 정보가 필요합니다. " + provider.name() + " 로그인 시 프로필 정보 제공에 동의해주세요."
            );
        }

        // ProviderId 필수 검증
        if (userInfo.getProviderId() == null || userInfo.getProviderId().isBlank()) {
            log.error("OAuth2 사용자 정보에 ProviderId가 없음: provider={}", provider);
            throw new BusinessException(
                ErrorCode.OAUTH2_AUTHENTICATION_FAILED,
                "사용자 식별 정보를 가져올 수 없습니다."
            );
        }
    }
}
