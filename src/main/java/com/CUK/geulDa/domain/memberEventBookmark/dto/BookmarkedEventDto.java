package com.CUK.geulDa.domain.memberEventBookmark.dto;

import com.CUK.geulDa.domain.event.Event;

public record BookmarkedEventDto(
        String eventId,
        String title,
        String body
) {
    public BookmarkedEventDto(Event event) {
        this(event.getId(), event.getTitle(), event.getBody());
    }
}
