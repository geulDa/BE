package com.CUK.geulDa.domain.place.dto;

import java.util.List;

public record PlaceListResponse(
        List<PlaceItem> places
) {
    public static PlaceListResponse of(List<PlaceItem> places) {
        return new PlaceListResponse(places);
    }

    public record PlaceItem(
            Long placeId,
            String name,
            Boolean hasStamp
    ) {
        public static PlaceItem of(Long placeId, String name, Boolean hasStamp) {
            return new PlaceItem(placeId, name, hasStamp);
        }
    }
}