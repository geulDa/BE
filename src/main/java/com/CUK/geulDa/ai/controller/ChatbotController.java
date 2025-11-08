package com.CUK.geulDa.ai.controller;

import com.CUK.geulDa.ai.dto.ChatMessageRequest;
import com.CUK.geulDa.ai.dto.ChatResponse;
import com.CUK.geulDa.ai.service.ChatbotService;
import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "AI 챗봇", description = "RAG 기반 부천 관광 안내 챗봇 API")
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;

    @Operation(summary = "채팅 세션 생성", description = "새로운 채팅 세션을 생성합니다")
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createSession() {
        String sessionId = chatbotService.createSession();
        Map<String, Object> sessionData = Map.of(
                "sessionId", sessionId,
                "expiresAt", LocalDateTime.now().plusHours(1)
        );
        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.SUCCESS_AI_CHATBOT_SESSION_CREATE, sessionData)
        );
    }

    @Operation(
            summary = "챗봇 대화",
            description = "챗봇과 대화를 진행합니다. 세션 ID는 X-Chat-Session 헤더로 전달해야 합니다."
    )
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Parameter(description = "채팅 세션 ID", required = true)
            @RequestHeader(value = "X-Chat-Session") String sessionId,
            @Valid @RequestBody ChatMessageRequest request) {
        log.info("챗봇 요청: session={}, message={}", sessionId, request.message());
        ChatResponse response = chatbotService.chat(sessionId, request.message());
        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.SUCCESS_AI_CHATBOT_CHAT, response)
        );
    }
}
