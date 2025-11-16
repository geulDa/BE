package com.CUK.geulDa.ai.service.util;

import com.CUK.geulDa.ai.dto.CourseRecommendResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DistanceCalculator {

    private static final int EARTH_RADIUS_KM = 6371;

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public double calculateTotalDistance(List<CourseRecommendResponse.PlaceDetail> places,
                                         double startLat, double startLon) {
        if (places.isEmpty()) {
            return 0.0;
        }

        double totalDistance = 0.0;
        double prevLat = startLat;
        double prevLon = startLon;

        for (CourseRecommendResponse.PlaceDetail place : places) {
            totalDistance += calculateDistance(prevLat, prevLon,
                    place.latitude(), place.longitude());
            prevLat = place.latitude();
            prevLon = place.longitude();
        }

        return Math.round(totalDistance * 100.0) / 100.0;
    }
}
