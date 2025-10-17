package com.CUK.geulDa.domain.place;

import com.CUK.geulDa.domain.postcard.PostCard;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class Place {

    @Id
    private String id;

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

    protected Place() {}

    public Place(String id, String name, String description, String address,
                 Double latitude, Double longitude, Boolean isHidden, String video, String placeImg) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isHidden = isHidden;
        this.video = video;
        this.placeImg = placeImg;
    }

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostCard> postcards = new ArrayList<>();
}
