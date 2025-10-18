package com.CUK.geulDa.domain.course;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Course extends BaseEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private String travelPurpose;

    private String stayDuration;

    private String transportMode;

    private String desiredPlaces;

    private String aiResult;

    // 생성자
    protected Course() {}

    // setter
    public Course(String id, Member member, String travelPurpose,
                                String stayDuration, String transportMode,
                                String desiredPlaces, String aiResult) {
        this.id = id;
        this.member = member;
        this.travelPurpose = travelPurpose;
        this.stayDuration = stayDuration;
        this.transportMode = transportMode;
        this.desiredPlaces = desiredPlaces;
    }

}
