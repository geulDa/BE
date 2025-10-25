package com.CUK.geulDa.domain.course;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private String travelPurpose;

    private String stayDuration;

    private String transportMode;

    @Column(columnDefinition = "TEXT")
    private String desiredPlaces;

    @Column(columnDefinition = "TEXT")
    private String aiResult;

    @Builder
    public Course(Member member, String travelPurpose, String stayDuration,
                  String transportMode, String desiredPlaces, String aiResult) {
        this.member = member;
        this.travelPurpose = travelPurpose;
        this.stayDuration = stayDuration;
        this.transportMode = transportMode;
        this.desiredPlaces = desiredPlaces;
        this.aiResult = aiResult;
    }
}
