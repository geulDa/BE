package com.CUK.geulDa.domain.stamp;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.place.Place;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Stamp {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    private Boolean isCompleted = false;

    protected Stamp() {}

    public Stamp(String id, Member member, Place place) {
        this.id = id;
        this.member = member;
        this.place = place;
    }

    public void markCompleted() {
        this.isCompleted = true;
    }
}