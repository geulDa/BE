package com.CUK.geulDa.domain.place.controller;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.place.dto.PlaceDetailResponse;
import com.CUK.geulDa.domain.place.dto.PlaceListResponse;
import com.CUK.geulDa.domain.place.service.PlaceService;
import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;
import com.CUK.geulDa.global.auth.annotation.CurrentMember;
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

    @Operation(summary = "(메인화면 지도) 명소 리스트 조회", description = "모든 명소 리스트를 조회합니다. 각 명소마다 스탬프 소유 여부를 포함합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PlaceListResponse>> getPlaceList(
            @CurrentMember Member member) {
        PlaceListResponse response = placeService.getPlaceList(member);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, response));
    }

    @Operation(summary = "명소 정보 조회", description = "명소 정보를 조회합니다. 완성된 스탬프가 있으면 상세 정보, 없으면 빈 정보를 반환합니다.")
    @GetMapping("/{placeId}")
    public ResponseEntity<ApiResponse<PlaceDetailResponse>> getPlaceDetail(
            @PathVariable Long placeId,
            @RequestParam Long memberId) {
        PlaceDetailResponse response = placeService.getPlaceDetail(placeId, memberId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, response));
    }
}
