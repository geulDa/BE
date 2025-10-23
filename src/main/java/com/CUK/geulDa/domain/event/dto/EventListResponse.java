package com.CUK.geulDa.domain.event.dto;

import com.CUK.geulDa.domain.event.Event;

public record EventListResponse(
        String eventId,
        String title,
        String body,
        Boolean isBookmarked
) {
    public EventListResponse(Event event, Boolean isBookmarked) {
        this(
                event.getId(),
                event.getTitle(),
                event.getBody(),
                isBookmarked
        );
    }
}
