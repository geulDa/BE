package com.CUK.geulDa.domain.stamp.dto;

public record StampAcquireResponse(
        Long stampId,
        Long memberId,
        String video,
        String systemMessage,
        PostcardInfo postcard
) {
    public record PostcardInfo(
            String imageUrl,
            String placeName,
            String description,
            String address,
            Boolean hidden
    ) {
    }
}
