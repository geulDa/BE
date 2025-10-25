package com.CUK.geulDa.domain.member.service;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.member.dto.MypageResponse;
import com.CUK.geulDa.domain.member.repository.MemberRepository;
import com.CUK.geulDa.domain.memberEventBookmark.MemberEventBookmark;
import com.CUK.geulDa.domain.memberEventBookmark.repository.MemberEventBookmarkRepository;
import com.CUK.geulDa.domain.postcard.UserPostCard;
import com.CUK.geulDa.domain.postcard.repository.UserPostCardRepository;
import com.CUK.geulDa.global.apiReponse.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberEventBookmarkRepository bookmarkRepository;
    private final UserPostCardRepository postCardRepository;

    public MypageResponse getMypage(String memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException(memberId));

        List<MemberEventBookmark> bookmarks = bookmarkRepository.findByMemberIdWithEvent(memberId);
        List<UserPostCard> postcards = postCardRepository.findByMemberIdWithPostCard(memberId);

        return new MypageResponse(member, bookmarks, postcards);
    }
}
