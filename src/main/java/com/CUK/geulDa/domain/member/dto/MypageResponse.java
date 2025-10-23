package com.CUK.geulDa.domain.member.dto;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.memberEventBookmark.MemberEventBookmark;
import com.CUK.geulDa.domain.memberEventBookmark.dto.BookmarkedEventDto;
import com.CUK.geulDa.domain.postcard.UserPostCard;
import com.CUK.geulDa.domain.postcard.dto.PostCardDto;

import java.util.List;
import java.util.stream.Collectors;

public record MypageResponse(
        String memberId,
        String name,
        String profileImageUrl,
        List<BookmarkedEventDto> bookmarkedEvents,
        List<PostCardDto> postcards
) {
    public MypageResponse(Member member, List<MemberEventBookmark> bookmarks, List<UserPostCard> postcards) {
        this(
                member.getId(),
                member.getName(),
                member.getProfileImageUrl(),
                bookmarks.stream()
                        .map(bookmark -> new BookmarkedEventDto(bookmark.getEvent()))
                        .collect(Collectors.toList()),
                postcards.stream()
                        .map(userPostCard -> new PostCardDto(userPostCard.getPostcard()))
                        .collect(Collectors.toList())
        );
    }
}
