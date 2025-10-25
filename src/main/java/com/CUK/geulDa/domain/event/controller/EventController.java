package com.CUK.geulDa.domain.event.controller;

import com.CUK.geulDa.domain.event.dto.EventDetailResponse;
import com.CUK.geulDa.domain.event.dto.EventListResponse;
import com.CUK.geulDa.domain.event.service.EventService;
import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Event", description = "행사 API")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "날짜별 행사 목록 조회", description = "특정 날짜에 열려있는 행사 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventListResponse>>> getEventsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long memberId) {
        List<EventListResponse> events = eventService.getEventsByDate(date, memberId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, events));
    }

    @Operation(summary = "행사 상세 조회", description = "행사의 상세 정보를 조회합니다.")
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> getEventDetail(@PathVariable Long eventId) {
        EventDetailResponse event = eventService.getEventDetail(eventId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, event));
    }
}
