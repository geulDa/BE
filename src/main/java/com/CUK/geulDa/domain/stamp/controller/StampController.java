package com.CUK.geulDa.domain.stamp.controller;

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
}