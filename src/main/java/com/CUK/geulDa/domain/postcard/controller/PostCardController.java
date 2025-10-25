package com.CUK.geulDa.domain.postcard.controller;

import com.CUK.geulDa.domain.postcard.dto.PostCardDetailResponse;
import com.CUK.geulDa.domain.postcard.service.PostCardService;
import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/postcards")
@RequiredArgsConstructor
@Tag(name = "PostCard", description = "엽서 API")
public class PostCardController {

    private final PostCardService postCardService;

    @Operation(summary = "엽서 상세 조회", description = "엽서의 상세 정보를 조회합니다.")
    @GetMapping("/{postcardId}")
    public ResponseEntity<ApiResponse<PostCardDetailResponse>> getPostCardDetail(@PathVariable Long postcardId) {
        PostCardDetailResponse postCard = postCardService.getPostCardDetail(postcardId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, postCard));
    }
}
