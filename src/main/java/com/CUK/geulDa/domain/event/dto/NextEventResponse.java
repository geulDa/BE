package com.CUK.geulDa.domain.event.dto;

import com.CUK.geulDa.domain.event.Event;

public record NextEventResponse(
        String eventId,
        String title,
        String imageUrl
) {
    public NextEventResponse(Event event) {
        this(
                event.getId(),
                event.getTitle(),
                event.getImageUrl()
        );
    }
}