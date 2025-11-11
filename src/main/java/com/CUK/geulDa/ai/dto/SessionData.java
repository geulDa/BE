package com.CUK.geulDa.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class SessionData implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("memberId")
    private final Long memberId;

    @JsonProperty("places")
    private final List<CourseRecommendResponse.PlaceDetail> places;

    @JsonProperty("createdAt")
    private final LocalDateTime createdAt;

    @JsonProperty("travelPurpose")
    private final String travelPurpose;

    @JsonProperty("stayDuration")
    private final String stayDuration;

    @JsonProperty("transportation")
    private final String transportation;

    @JsonCreator
    public SessionData(
        @JsonProperty("memberId") Long memberId,
        @JsonProperty("places") List<CourseRecommendResponse.PlaceDetail> places,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("travelPurpose") String travelPurpose,
        @JsonProperty("stayDuration") String stayDuration,
        @JsonProperty("transportation") String transportation) {
        this.memberId = memberId;
        this.places = places;
        this.createdAt = createdAt;
        this.travelPurpose = travelPurpose;
        this.stayDuration = stayDuration;
        this.transportation = transportation;
    }

    public Long getMemberId() {
        return memberId;
    }

    public List<CourseRecommendResponse.PlaceDetail> getPlaces() {
        return places;
    }
}