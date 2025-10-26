package com.CUK.geulDa.domain.postcard;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.place.Place;
import com.CUK.geulDa.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPostCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postcard_id", nullable = false)
    private PostCard postcard;

    private LocalDateTime issuedAt;

    @Builder
    public UserPostCard(Member member, Place place, PostCard postcard) {
        this.member = member;
        this.place = place;
        this.postcard = postcard;
        this.issuedAt = LocalDateTime.now();
    }
}