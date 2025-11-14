package com.CUK.geulDa.domain.place;

import com.CUK.geulDa.domain.postcard.PostCard;
import com.CUK.geulDa.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseEntity {

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

    // 장소 이미지
    private String placeImg;

    // 스탬프 획득 시 시스템 메시지
    @Column(columnDefinition = "TEXT")
    private String systemMessage;

    // 연관관계
    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostCard> postcards = new ArrayList<>();

    @Builder
    public Place(String name, String description, String address,
        Double latitude, Double longitude, Boolean isHidden,
        String video, String placeImg, String systemMessage) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isHidden = isHidden;
        this.video = video;
        this.placeImg = placeImg;
        this.systemMessage = systemMessage;
    }
}
