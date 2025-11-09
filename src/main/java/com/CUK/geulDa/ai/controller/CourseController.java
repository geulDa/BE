package com.CUK.geulDa.ai.controller;

import com.CUK.geulDa.ai.dto.CourseRecommendResponse;
import com.CUK.geulDa.ai.dto.RecommendRequest;
import com.CUK.geulDa.ai.service.CourseRecommendService;
import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;
import com.CUK.geulDa.global.auth.annotation.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI 코스 추천", description = "AI를 활용한 개인화 관광 코스 추천 API")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseRecommendService courseService;
    private final RedisTemplate<String, Object> redisTemplate;

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSession(
            @PathVariable String sessionId) {
        String key = "ai:session:" + sessionId;
        Object session = redisTemplate.opsForValue().get(key);

        if (session == null) {
            throw new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND,
                    "세션 ID: " + sessionId);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> sessionData = (Map<String, Object>) session;
        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.SUCCESS_AI_SESSION_RETRIEVE, sessionData)
        );
    }
}
