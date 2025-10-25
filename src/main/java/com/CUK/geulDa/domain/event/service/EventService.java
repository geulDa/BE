package com.CUK.geulDa.domain.event.service;

import com.CUK.geulDa.domain.event.Event;
import com.CUK.geulDa.domain.event.dto.EventDetailResponse;
import com.CUK.geulDa.domain.event.dto.EventListResponse;
import com.CUK.geulDa.domain.event.dto.NextEventResponse;
import com.CUK.geulDa.domain.event.repository.EventRepository;
import com.CUK.geulDa.domain.memberEventBookmark.MemberEventBookmark;
import com.CUK.geulDa.domain.memberEventBookmark.repository.MemberEventBookmarkRepository;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final MemberEventBookmarkRepository bookmarkRepository;

    /**
     * 행사 상세 조회
     * @param eventId 행사 ID
     * @return 행사 상세 정보
     */
    public EventDetailResponse getEventDetail(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "ID가 " + eventId + "인 행사를 찾을 수 없습니다."
                ));

        List<NextEventResponse> nextEvents = getNextEvents(event);

        return new EventDetailResponse(event, nextEvents);
    }


    private List<NextEventResponse> getNextEvents(Event currentEvent) {
        List<Event> eventsOnSameDate = eventRepository.findByDate(currentEvent.getStartDate());

        if (eventsOnSameDate.size() <= 1) {
            return new ArrayList<>();
        }

        int currentIndex = -1;
        for (int i = 0; i < eventsOnSameDate.size(); i++) {
            if (eventsOnSameDate.get(i).getId().equals(currentEvent.getId())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            return new ArrayList<>();
        }

        List<NextEventResponse> nextEvents = new ArrayList<>();
        int totalEvents = eventsOnSameDate.size();

        for (int i = 1; i <= 2 && i < totalEvents; i++) {
            int nextIndex = (currentIndex + i) % totalEvents;
            Event nextEvent = eventsOnSameDate.get(nextIndex);
            nextEvents.add(new NextEventResponse(nextEvent));
        }

        return nextEvents;
    }

    public List<EventListResponse> getEventsByDate(LocalDate targetDate, Long memberId) {
        List<Event> events = eventRepository.findByDate(targetDate);

        List<MemberEventBookmark> bookmarks = bookmarkRepository.findByMemberIdWithEvent(memberId);
        Set<Long> bookmarkedEventIds = bookmarks.stream()
                .map(bookmark -> bookmark.getEvent().getId())
                .collect(Collectors.toSet());

        return events.stream()
                .map(event -> new EventListResponse(event, bookmarkedEventIds.contains(event.getId())))
                .collect(Collectors.toList());
    }
}
