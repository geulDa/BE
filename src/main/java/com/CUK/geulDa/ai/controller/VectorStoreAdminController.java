package com.CUK.geulDa.ai.controller;

import com.CUK.geulDa.ai.service.ChatbotService;
import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "관리자 - 벡터 스토어", description = "벡터 스토어 관리 API (장소 데이터 변경 시 사용)")
@RestController
@RequestMapping("/api/admin/vector-store")
@RequiredArgsConstructor
@Slf4j
public class VectorStoreAdminController {

    private final ChatbotService chatbotService;

    @Operation(
        summary = "벡터 스토어 재생성",
        description = """
            장소 데이터가 변경되었을 때 벡터 스토어를 재생성합니다.

            **주의:** 장소 개수에 따라 시간이 오래 걸릴 수 있습니다.
            - 100개 장소: 약 40~50초
            - 200개 장소: 약 80~100초

            **사용 시나리오:**
            1. TourAPI에서 새로운 장소 데이터를 DB에 추가한 경우
            2. 기존 장소의 설명(description)을 수정한 경우
            3. 장소를 삭제하거나 숨김 처리한 경우

            **재생성 후:**
            - vector-store.json 파일이 업데이트됩니다
            - 다음 서버 재시작 시 새로운 벡터 데이터가 로드됩니다
            """
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshVectorStore() {
        log.info("벡터 스토어 재생성 요청 수신");

        chatbotService.refreshVectorStore();

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.SUCCESS_CREATE,
                        Map.of(
                                "message", "벡터 스토어 재생성이 완료되었습니다.",
                                "notice", "변경 사항은 다음 서버 재시작 시 반영됩니다."
                        )
                )
        );
    }
}
