package com.CUK.geulDa.domain.place.controller;

import com.CUK.geulDa.domain.place.dto.PlaceMigrationResult;
import com.CUK.geulDa.domain.place.service.PlaceImageMigrationService;
import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/places/migration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Place Migration (Admin)", description = "장소 이미지 마이그레이션 관리 API")
public class PlaceMigrationController {

    private final PlaceImageMigrationService migrationService;

    @PostMapping("/images")
    @Operation(summary = "전체 장소 이미지 마이그레이션")
    public ResponseEntity<ApiResponse<PlaceMigrationResult>> migrateAllImages() {
        log.info("전체 장소 이미지 마이그레이션 요청");
        PlaceMigrationResult result = migrationService.migrateAllPlaceImages();
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_UPDATE, result));
    }

    @PostMapping("/images/{placeId}")
    @Operation(summary = "단일 장소 이미지 마이그레이션")
    public ResponseEntity<ApiResponse<PlaceMigrationResult>> migrateSingleImage(@PathVariable Long placeId) {
        PlaceMigrationResult result = migrationService.migrateSinglePlace(placeId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_UPDATE, result));
    }
}
