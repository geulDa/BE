package com.CUK.geulDa.domain.event.dto;

import com.CUK.geulDa.domain.event.Event;

import java.time.LocalDate;
import java.util.List;

public record EventDetailResponse(
        Long eventId,
        String title,
        String body,
        String imageUrl,
        String address,
        LocalDate startDate,
        LocalDate endDate,
        String externalUrl,
        List<NextEventResponse> nextEvents
) {
    public EventDetailResponse(Event event, List<NextEventResponse> nextEvents) {
        this(
                event.getId(),
                event.getTitle(),
                event.getBody(),
                event.getImageUrl(),
                event.getAddress(),
                event.getStartDate(),
                event.getEndDate(),
                event.getExternalUrl(),
                nextEvents
        );
    }
}
