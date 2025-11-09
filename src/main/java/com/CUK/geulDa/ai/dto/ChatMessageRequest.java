package com.CUK.geulDa.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
        @NotBlank(message = "메시지는 필수입니다")
        String message
) {
}
