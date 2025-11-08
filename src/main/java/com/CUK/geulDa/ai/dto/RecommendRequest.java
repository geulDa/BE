package com.CUK.geulDa.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record RecommendRequest(
        @NotBlank(message = "여행 목적은 필수입니다")
        String travelPurpose,

        @NotBlank(message = "체류 기간은 필수입니다")
        String stayDuration,

        @NotBlank(message = "이동 수단은 필수입니다")
        String transportation,

        Double userLatitude,

        Double userLongitude,

        String mustVisitPlace
) {
}
