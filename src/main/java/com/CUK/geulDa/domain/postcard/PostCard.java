package com.CUK.geulDa.domain.postcard;

import com.CUK.geulDa.domain.place.Place;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class PostCard {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    private String message;

    private String imageUrl;

    private Boolean isHidden;

    protected PostCard() {}

    public PostCard(String id, Place place, String message,
                    String imageUrl, Boolean isHidden, Double dropRate) {
        this.id = id;
        this.place = place;
        this.message = message;
        this.imageUrl = imageUrl;
        this.isHidden = isHidden;
    }
}