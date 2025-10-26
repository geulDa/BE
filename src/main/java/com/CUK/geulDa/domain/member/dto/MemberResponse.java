package com.CUK.geulDa.domain.member.dto;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.member.constant.Provider;
import com.CUK.geulDa.domain.member.constant.Role;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String email,
        String name,
        String profileImageUrl,
        Provider provider,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getProfileImageUrl(),
                member.getProvider(),
                member.getRole(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
