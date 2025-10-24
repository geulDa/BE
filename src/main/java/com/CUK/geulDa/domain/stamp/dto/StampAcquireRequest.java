package com.CUK.geulDa.domain.stamp.dto;

public record StampAcquireRequest(
        String memberId,
        Double latitude,
        Double longitude
) {
}
