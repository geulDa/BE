package com.CUK.geulDa.global.apiReponse.code;

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
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "E502", "외부 API 호출에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
