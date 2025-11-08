package com.CUK.geulDa.ai.dto;

import java.util.List;

public record CourseRecommendResponse(
        String sessionId,
        List<PlaceDetail> places,
        String routeSummary,
        Double totalDistance
) {
    public CourseRecommendResponse {
        places = List.copyOf(places);
    }

    public record PlaceDetail(
            Long placeId,
            String name,
            String address,
            Double latitude,
            Double longitude,
            String description,
            String placeImg
    ) {
    }
}
