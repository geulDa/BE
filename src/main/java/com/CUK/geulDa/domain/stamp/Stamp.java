package com.CUK.geulDa.domain.stamp;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.place.Place;
import com.CUK.geulDa.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Stamp extends BaseEntity {

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

    public void visited() {
        this.isCompleted = true;
    }
}