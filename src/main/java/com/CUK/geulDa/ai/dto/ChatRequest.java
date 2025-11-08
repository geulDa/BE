package com.CUK.geulDa.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "세션 ID는 필수입니다")
        String sessionId,

        @NotBlank(message = "메시지는 필수입니다")
        String message
) {
}
