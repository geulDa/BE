package com.CUK.geulDa.domain.postcard.dto;

import com.CUK.geulDa.domain.postcard.PostCard;

public record PostCardDetailResponse(
        String postcardId,
        String imageUrl,
        String placeName,
        String placeDescription
) {
    public PostCardDetailResponse(PostCard postCard) {
        this(
                postCard.getId(),
                postCard.getImageUrl(),
                postCard.getPlace().getName(),
                postCard.getPlace().getDescription()
        );
    }
}
