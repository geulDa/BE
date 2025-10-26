package com.CUK.geulDa.domain.postcard;

import com.CUK.geulDa.domain.place.Place;
import com.CUK.geulDa.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String imageUrl;

    private Boolean isHidden;

    @Builder
    public PostCard(Place place, String message, String imageUrl, Boolean isHidden) {
        this.place = place;
        this.message = message;
        this.imageUrl = imageUrl;
        this.isHidden = isHidden;
    }
}