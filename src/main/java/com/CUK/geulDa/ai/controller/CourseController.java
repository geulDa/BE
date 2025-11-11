package com.CUK.geulDa.ai.controller;

import com.CUK.geulDa.ai.dto.CourseRecommendResponse;
import com.CUK.geulDa.ai.dto.RecommendRequest;
import com.CUK.geulDa.ai.dto.SessionData;
import com.CUK.geulDa.ai.service.CourseRecommendService;
import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;
import com.CUK.geulDa.global.auth.annotation.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 코스 추천", description = "AI를 활용한 개인화 관광 코스 추천 API")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseRecommendService courseService;

    @Operation(summary = "코스 추천", description = "사용자 위치와 선호도를 기반으로 관광 코스를 추천합니다 (JWT 토큰 필요)")
    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<CourseRecommendResponse>> recommend(
            @CurrentMember Member member,
            @Valid @RequestBody RecommendRequest request) {
        log.info("코스 추천 요청: 사용자={}, {}", member.getId(), request);
        CourseRecommendResponse response = courseService.recommend(member, request);
        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.SUCCESS_AI_COURSE_RECOMMEND, response)
        );
    }

    @Operation(summary = "세션 조회", description = "저장된 추천 세션을 조회합니다")
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<SessionData>> getSession(
            @PathVariable String sessionId) {
        log.info("세션 조회 요청: sessionId={}", sessionId);
        SessionData sessionData = courseService.getSession(sessionId);
        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.SUCCESS_AI_SESSION_RETRIEVE, sessionData)
        );
    }
}
