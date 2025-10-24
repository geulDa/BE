package com.CUK.geulDa.domain.stamp.dto;

public record StampAcquireResponse(
        String stampId,
        String video,
        PostcardInfo postcard
) {
    public record PostcardInfo(
            String imageUrl,
            String placeName,
            String description,
            String address
    ) {
    }
}
