package com.CUK.geulDa.domain.member.repository;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.member.constant.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * Provider와 ProviderId로 회원 조회 (OAuth 로그인용)
     */
    Optional<Member> findByProviderAndProviderId(Provider provider, String providerId);
}
