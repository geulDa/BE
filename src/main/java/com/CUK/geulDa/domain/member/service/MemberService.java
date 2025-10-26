package com.CUK.geulDa.domain.member.service;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.member.constant.Provider;
import com.CUK.geulDa.domain.member.constant.Role;
import com.CUK.geulDa.domain.member.dto.MypageResponse;
import com.CUK.geulDa.domain.member.repository.MemberRepository;
import com.CUK.geulDa.domain.memberEventBookmark.MemberEventBookmark;
import com.CUK.geulDa.domain.memberEventBookmark.repository.MemberEventBookmarkRepository;
import com.CUK.geulDa.domain.postcard.UserPostCard;
import com.CUK.geulDa.domain.postcard.repository.UserPostCardRepository;
import com.CUK.geulDa.global.apiResponse.exception.UserNotFoundException;
import com.CUK.geulDa.global.auth.oidc.OidcUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberEventBookmarkRepository bookmarkRepository;
    private final UserPostCardRepository postCardRepository;

    /**
     * 마이페이지 조회
     */
    public MypageResponse getMypage(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException(String.valueOf(memberId)));

        List<MemberEventBookmark> bookmarks = bookmarkRepository.findByMemberIdWithEvent(memberId);
        List<UserPostCard> postcards = postCardRepository.findByMemberIdWithPostCard(memberId);

        return new MypageResponse(member, bookmarks, postcards);
    }

    /**
     * OAuth2 사용자 정보로 회원 조회 또는 생성
     * - 기존 회원이면 프로필 정보 업데이트
     * - 신규 회원이면 회원 생성
     */
    @Transactional
    public Member getOrCreateMember(OidcUserInfo userInfo, Provider provider) {
        return memberRepository.findByProviderAndProviderId(provider, userInfo.getProviderId())
                .map(existingMember -> {
                    // 기존 회원 - 프로필 정보 업데이트
                    log.info("기존 회원 로그인: memberId={}, provider={}", existingMember.getId(), provider);
                    existingMember.updateProfile(userInfo.getName(), userInfo.getProfileImageUrl());
                    return existingMember;
                })
                .orElseGet(() -> {
                    // 신규 회원 생성
                    Member newMember = Member.builder()
                            .email(userInfo.getEmail())
                            .name(userInfo.getName())
                            .profileImageUrl(userInfo.getProfileImageUrl())
                            .provider(provider)
                            .providerId(userInfo.getProviderId())
                            .role(Role.USER)
                            .build();
                    Member savedMember = memberRepository.save(newMember);
                    log.info("신규 회원 가입: memberId={}, email={}, provider={}",
                            savedMember.getId(), savedMember.getEmail(), provider);
                    return savedMember;
                });
    }

    /**
     * 회원 ID로 회원 조회
     */
    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException(String.valueOf(memberId)));
    }
}
