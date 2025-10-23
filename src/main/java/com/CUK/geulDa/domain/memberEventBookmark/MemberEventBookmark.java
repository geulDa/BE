package com.CUK.geulDa.domain.memberEventBookmark;

import com.CUK.geulDa.domain.event.Event;
import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class MemberEventBookmark extends BaseEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    private LocalDateTime bookmarkedAt;

    protected MemberEventBookmark() {}

    public MemberEventBookmark(String id, Member member, Event event) {
        this.id = id;
        this.member = member;
        this.event = event;
        this.bookmarkedAt = LocalDateTime.now();
    }
}