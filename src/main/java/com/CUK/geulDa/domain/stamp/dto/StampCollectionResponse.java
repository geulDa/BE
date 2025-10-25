package com.CUK.geulDa.domain.stamp.dto;

import java.util.List;

public record StampCollectionResponse(
        long totalStampCount,
        long collectedStampCount,
        List<String> stampIds
) {
    public static StampCollectionResponse of(long totalStampCount, long collectedStampCount, List<String> stampIds) {
        return new StampCollectionResponse(totalStampCount, collectedStampCount, stampIds);
    }
}