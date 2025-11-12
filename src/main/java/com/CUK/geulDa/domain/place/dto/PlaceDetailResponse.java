package com.CUK.geulDa.domain.place.dto;

public record PlaceDetailResponse(
        Long placeId,
        Boolean isCompleted,
        String imageUrl,
        String placeName,
        String description,
        String address
) {
    public static PlaceDetailResponse completed(Long placeId, String imageUrl, String placeName, String description, String address) {
        return new PlaceDetailResponse(placeId, true, imageUrl, placeName, description, address);
    }

    public static PlaceDetailResponse incomplete(Long placeId,String placeName, String description, String address) {
        return new PlaceDetailResponse(placeId, false, null, placeName, description, address);
    }
}