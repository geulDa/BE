package com.CUK.geulDa.global.auth.controller;

import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;
import com.CUK.geulDa.global.auth.dto.LogoutRequest;
import com.CUK.geulDa.global.auth.dto.RefreshTokenRequest;
import com.CUK.geulDa.global.auth.dto.TempTokenRequest;
import com.CUK.geulDa.global.auth.dto.TokenResponse;
import com.CUK.geulDa.global.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "인증 관련 API")
public class AuthController {

    private final TokenService tokenService;

    @Operation(summary = "임시 토큰 교환", description = "OAuth2 인증 후 받은 임시 토큰을 실제 JWT로 교환합니다.")
    @PostMapping("/temp-token/exchange")
    public ApiResponse<TokenResponse> exchangeTempToken(@Valid @RequestBody TempTokenRequest request) {
        TokenResponse tokenResponse = tokenService.exchangeTempToken(request.tempToken());
        return ApiResponse.success(SuccessCode.SUCCESS_LOGIN, tokenResponse);
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access Token과 Refresh Token을 발급받습니다.")
    @PostMapping("/token/refresh")
    public ApiResponse<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = tokenService.rotateRefreshToken(request.refreshToken());
        return ApiResponse.success(SuccessCode.SUCCESS_TOKEN_REFRESH, tokenResponse);
    }

    @Operation(summary = "로그아웃", description = "Access Token을 무효화하고 Refresh Token을 삭제합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody LogoutRequest request
    ) {
        tokenService.logout(authorization, request.refreshToken());
        return ApiResponse.success(SuccessCode.SUCCESS_LOGOUT);
    }
}
