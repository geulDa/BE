package com.CUK.geulDa.domain.course;

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

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;

    // 위도
    private Double latitude;

    // 경도
    private Double longitude;

    // 히든
    private Boolean isHidden;

    // 비디오
    private String video;

    @Column(columnDefinition = "TEXT")
    private String placeImg;

    // 스탬프 획득 시 시스템 메시지
    @Column(columnDefinition = "TEXT")
    private String systemMessage;

    // 카테고리
    @Column(name = "category")
    private String category;

    // 관광 목적 태그
    @Column(name = "tour_purpose_tags")
    private String tourPurposeTags;

    // 권장 방문 시간
    @Column(name = "recommended_duration")
    private Integer recommendedDuration;

    // 인기도 점수 (0-100, 기본값 50)
    @Column(name = "popularity_score")
    private Integer popularityScore = 50;

    // 데이터 출처
    @Column(name = "data_source")
    private String dataSource = "manual";

    @Builder
    public Course(String name, String description, String address,
                  Double latitude, Double longitude, Boolean isHidden,
                  String video, String placeImg, String systemMessage,
                  String category, String tourPurposeTags, Integer recommendedDuration,
                  Integer popularityScore, String dataSource) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isHidden = isHidden;
        this.video = video;
        this.placeImg = placeImg;
        this.systemMessage = systemMessage;
        this.category = category;
        this.tourPurposeTags = tourPurposeTags;
        this.recommendedDuration = recommendedDuration;
        this.popularityScore = popularityScore != null ? popularityScore : 50;
        this.dataSource = dataSource != null ? dataSource : "manual";
    }
}
