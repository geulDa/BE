package com.CUK.geulDa.global.apiResponse.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 사용자 (E001~E099)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_USER(HttpStatus.CONFLICT, "E002", "이미 존재하는 사용자입니다."),
    USER_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "E003", "해당 작업을 수행할 권한이 없습니다."),

    // 인증/인가 (E100~E199)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E100", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "E101", "접근 권한이 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "E102", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "E103", "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "E104", "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "E105", "Refresh Token 재사용이 감지되었습니다. 보안을 위해 로그아웃 처리됩니다."),
    INVALID_TEMP_TOKEN(HttpStatus.UNAUTHORIZED, "E106", "유효하지 않거나 만료된 임시 토큰입니다."),
    OAUTH2_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "E107", "OAuth2 인증에 실패했습니다."),
    UNSUPPORTED_OAUTH2_PROVIDER(HttpStatus.BAD_REQUEST, "E108", "지원하지 않는 OAuth2 Provider입니다."),

    // 입력값 검증 (E200~E299)
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "E200", "입력값이 올바르지 않습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "E201", "필수 파라미터가 누락되었습니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "E202", "입력값 검증에 실패했습니다."),

    // 리소스 (E300~E399)
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E300", "요청한 리소스를 찾을 수 없습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "E301", "이미 존재하는 리소스입니다."),
    RESOURCE_ALREADY_DELETED(HttpStatus.GONE, "E302", "이미 삭제된 리소스입니다."),

    // 서버 오류 (E500~E599)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "서버 내부 오류가 발생했습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E501", "데이터베이스 처리 중 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "E502", "외부 API 호출에 실패했습니다."),

    // AI 기능 오류 (E600~E699)
    AI_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E600", "AI 서비스 처리 중 오류가 발생했습니다."),
    AI_MODEL_ERROR(HttpStatus.BAD_GATEWAY, "E601", "AI 모델 호출에 실패했습니다."),
    AI_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "E602", "세션을 찾을 수 없습니다."),
    AI_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "E603", "AI 요청 형식이 올바르지 않습니다."),
    AI_NO_PLACES_FOUND(HttpStatus.NOT_FOUND, "E604", "검색된 장소가 없습니다."),
    VECTOR_STORE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E605", "벡터 스토어 처리 중 오류가 발생했습니다."),

    // Google Places API 오류 (E700~E799)
    GOOGLE_PLACES_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "E700", "Google Places API 할당량이 초과되었습니다."),
    GOOGLE_PLACES_API_ERROR(HttpStatus.BAD_GATEWAY, "E701", "Google Places API 호출에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
