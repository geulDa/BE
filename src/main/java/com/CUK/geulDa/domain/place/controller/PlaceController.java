package com.CUK.geulDa.domain.place.controller;

import com.CUK.geulDa.domain.place.dto.PlaceDetailResponse;
import com.CUK.geulDa.domain.place.service.PlaceService;
import com.CUK.geulDa.global.apiReponse.code.SuccessCode;
import com.CUK.geulDa.global.apiReponse.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
@Tag(name = "Place", description = "명소 API")
public class PlaceController {

    private final PlaceService placeService;

    @Operation(summary = "명소 정보 조회", description = "명소 정보를 조회합니다. 완성된 스탬프가 있으면 상세 정보, 없으면 빈 정보를 반환합니다.")
    @GetMapping("/{placeId}")
    public ResponseEntity<ApiResponse<PlaceDetailResponse>> getPlaceDetail(
            @PathVariable String placeId,
            @RequestParam String memberId) {
        PlaceDetailResponse response = placeService.getPlaceDetail(placeId, memberId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, response));
    }
}
