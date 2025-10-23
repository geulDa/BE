package com.CUK.geulDa.domain.event.controller;

import com.CUK.geulDa.domain.event.dto.EventDetailResponse;
import com.CUK.geulDa.domain.event.service.EventService;
import com.CUK.geulDa.global.apiReponse.code.SuccessCode;
import com.CUK.geulDa.global.apiReponse.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Event", description = "행사 API")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "행사 상세 조회", description = "행사의 상세 정보를 조회합니다.")
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> getEventDetail(@PathVariable String eventId) {
        EventDetailResponse event = eventService.getEventDetail(eventId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, event));
    }
}
