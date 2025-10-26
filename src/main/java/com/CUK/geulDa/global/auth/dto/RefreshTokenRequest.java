package com.CUK.geulDa.global.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {
}
