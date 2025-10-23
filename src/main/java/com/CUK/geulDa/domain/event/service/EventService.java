package com.CUK.geulDa.domain.event.service;

import com.CUK.geulDa.domain.event.Event;
import com.CUK.geulDa.domain.event.dto.EventDetailResponse;
import com.CUK.geulDa.domain.event.repository.EventRepository;
import com.CUK.geulDa.global.apiReponse.code.ErrorCode;
import com.CUK.geulDa.global.apiReponse.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    /**
     * 행사 상세 조회
     * @param eventId 행사 ID
     * @return 행사 상세 정보
     */
    public EventDetailResponse getEventDetail(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "ID가 " + eventId + "인 행사를 찾을 수 없습니다."
                ));

        return new EventDetailResponse(event);
    }
}
