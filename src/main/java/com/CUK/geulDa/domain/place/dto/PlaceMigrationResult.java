package com.CUK.geulDa.domain.place.dto;

import java.util.List;
import java.util.Map;

public record PlaceMigrationResult(
        int totalCount,
        int successCount,
        int failCount,
        int skippedCount,
        List<String> failedPlaces,
        Map<Long, String> updatedImages
) {
    public String getSummary() {
        return String.format(
                "전체: %d, 성공: %d, 실패: %d, 스킵: %d",
                totalCount, successCount, failCount, skippedCount
        );
    }
}
