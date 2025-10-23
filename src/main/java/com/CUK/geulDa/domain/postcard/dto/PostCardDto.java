package com.CUK.geulDa.domain.postcard.dto;

import com.CUK.geulDa.domain.postcard.PostCard;

public record PostCardDto(
        String postcardId,
        String imageUrl
) {
    public PostCardDto(PostCard postCard) {
        this(postCard.getId(), postCard.getImageUrl());
    }
}
