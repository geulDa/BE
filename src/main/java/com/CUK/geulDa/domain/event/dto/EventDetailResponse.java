package com.CUK.geulDa.domain.event.dto;

import com.CUK.geulDa.domain.event.Event;

import java.time.LocalDate;

public record EventDetailResponse(
        String eventId,
        String title,
        String body,
        String imageUrl,
        String address,
        LocalDate startDate,
        LocalDate endDate,
        String externalUrl
) {
    public EventDetailResponse(Event event) {
        this(
                event.getId(),
                event.getTitle(),
                event.getBody(),
                event.getImageUrl(),
                event.getAddress(),
                event.getStartDate(),
                event.getEndDate(),
                event.getExternalUrl()
        );
    }
}
