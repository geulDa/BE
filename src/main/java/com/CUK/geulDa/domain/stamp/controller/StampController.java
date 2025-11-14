package com.CUK.geulDa.domain.stamp.controller;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.stamp.dto.StampAcquireRequest;
import com.CUK.geulDa.domain.stamp.dto.StampAcquireResponse;
import com.CUK.geulDa.domain.stamp.dto.StampCollectionResponse;
import com.CUK.geulDa.domain.stamp.service.StampService;
import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;
import com.CUK.geulDa.global.auth.annotation.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stamps")
@RequiredArgsConstructor
@Tag(name = "Stamp", description = "스탬프 API")
public class StampController {

    private final StampService stampService;

    @Operation(
        summary = "스탬프 수집 현황 조회",
        description = "스탬프 수집 현황을 조회합니다. 로그인하지 않은 경우 빈 템플릿(10개 빈 공간)을 반환하고, 로그인한 경우 사용자가 모은 스탬프 정보를 반환합니다."
    )
    @GetMapping("/collection")
    public ResponseEntity<ApiResponse<StampCollectionResponse>> getStampCollection(
            @CurrentMember(required = false) Member member) {
        StampCollectionResponse response = stampService.getStampCollection(member);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, response));
    }

    @Operation(
        summary = "스탬프 획득",
        description = "현재 로그인한 사용자가 명소 5km 반경 내에서 스탬프를 획득하고 엽서를 발급받습니다. (JWT 토큰 필요)"
    )
    @PostMapping("/{placeId}/acquire")
    public ResponseEntity<ApiResponse<StampAcquireResponse>> acquireStamp(
            @PathVariable Long placeId,
            @CurrentMember Member member,
            @RequestBody StampAcquireRequest request) {
        StampAcquireResponse response = stampService.acquireStamp(
                placeId,
                member,
                request.latitude(),
                request.longitude()
        );
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_CREATE, response));
    }
}