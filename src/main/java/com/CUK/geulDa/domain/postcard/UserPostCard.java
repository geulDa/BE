package com.CUK.geulDa.domain.postcard;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.place.Place;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class UserPostCard {

    @Id
    private String id;

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

    protected UserPostCard() {}

    public UserPostCard(String id, Member member, Place place, PostCard postcard) {
        this.id = id;
        this.member = member;
        this.place = place;
        this.postcard = postcard;
        this.issuedAt = LocalDateTime.now();
    }
}