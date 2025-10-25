package com.CUK.geulDa.domain.stamp.controller;

import com.CUK.geulDa.domain.stamp.dto.StampAcquireRequest;
import com.CUK.geulDa.domain.stamp.dto.StampAcquireResponse;
import com.CUK.geulDa.domain.stamp.dto.StampCollectionResponse;
import com.CUK.geulDa.domain.stamp.service.StampService;
import com.CUK.geulDa.global.apiReponse.code.SuccessCode;
import com.CUK.geulDa.global.apiReponse.response.ApiResponse;
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

    @Operation(summary = "스탬프 수집 현황 조회", description = "사용자가 모은 스탬프 개수와 리스트를 조회합니다.")
    @GetMapping("/collection")
    public ResponseEntity<ApiResponse<StampCollectionResponse>> getStampCollection(
            @RequestParam String memberId) {
        StampCollectionResponse response = stampService.getStampCollection(memberId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, response));
    }

    @Operation(summary = "스탬프 획득", description = "명소 15m 반경 내에서 스탬프를 획득하고 엽서를 발급받습니다.")
    @PostMapping("/{placeId}/acquire")
    public ResponseEntity<ApiResponse<StampAcquireResponse>> acquireStamp(
            @PathVariable String placeId,
            @RequestBody StampAcquireRequest request) {
        StampAcquireResponse response = stampService.acquireStamp(
                placeId,
                request.memberId(),
                request.latitude(),
                request.longitude()
        );
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_CREATE, response));
    }
}