package com.CUK.geulDa.domain.place.dto;

public record PlaceDetailResponse(
        String placeId,
        Boolean isCompleted,
        String imageUrl,
        String placeName,
        String description,
        String address
) {
    public static PlaceDetailResponse completed(String placeId, String imageUrl, String placeName, String description, String address) {
        return new PlaceDetailResponse(placeId, true, imageUrl, placeName, description, address);
    }

    public static PlaceDetailResponse incomplete(String placeId) {
        return new PlaceDetailResponse(placeId, false, null, null, null, null);
    }
}