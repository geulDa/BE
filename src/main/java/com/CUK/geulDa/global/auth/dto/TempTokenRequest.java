package com.CUK.geulDa.global.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TempTokenRequest(
        @NotBlank(message = "임시 토큰은 필수입니다.")
        String tempToken
) {
}
