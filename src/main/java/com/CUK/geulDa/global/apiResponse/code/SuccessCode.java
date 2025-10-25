package com.CUK.geulDa.global.apiResponse.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {

    // 조회 (S001~S099)
    SUCCESS_READ(HttpStatus.OK, "S001", "조회에 성공했습니다."),
    SUCCESS_READ_USER(HttpStatus.OK, "S002", "사용자 정보 조회에 성공했습니다."),
    SUCCESS_READ_LIST(HttpStatus.OK, "S003", "목록 조회에 성공했습니다."),

    // 생성 (S100~S199)
    SUCCESS_CREATE(HttpStatus.CREATED, "S100", "생성에 성공했습니다."),
    SUCCESS_CREATE_USER(HttpStatus.CREATED, "S101", "사용자 등록에 성공했습니다."),
    SUCCESS_CREATE_RESOURCE(HttpStatus.CREATED, "S102", "리소스 생성에 성공했습니다."),

    // 수정 (S200~S299)
    SUCCESS_UPDATE(HttpStatus.OK, "S200", "수정에 성공했습니다."),
    SUCCESS_UPDATE_USER(HttpStatus.OK, "S201", "사용자 정보 수정에 성공했습니다."),
    SUCCESS_UPDATE_RESOURCE(HttpStatus.OK, "S202", "리소스 수정에 성공했습니다."),

    // 삭제 (S300~S399)
    SUCCESS_DELETE(HttpStatus.OK, "S300", "삭제에 성공했습니다."),
    SUCCESS_DELETE_USER(HttpStatus.OK, "S301", "사용자 삭제에 성공했습니다."),
    SUCCESS_DELETE_RESOURCE(HttpStatus.OK, "S302", "리소스 삭제에 성공했습니다."),

    // 인증/인가 (S400~S499)
    SUCCESS_LOGIN(HttpStatus.OK, "S400", "로그인에 성공했습니다."),
    SUCCESS_LOGOUT(HttpStatus.OK, "S401", "로그아웃에 성공했습니다."),
    SUCCESS_TOKEN_REFRESH(HttpStatus.OK, "S402", "토큰 갱신에 성공했습니다."),
    SUCCESS_SIGNUP(HttpStatus.CREATED, "S403", "회원가입에 성공했습니다."),

    // 기타 (S500~S599)
    SUCCESS_UPLOAD(HttpStatus.OK, "S500", "파일 업로드에 성공했습니다."),
    SUCCESS_DOWNLOAD(HttpStatus.OK, "S501", "파일 다운로드에 성공했습니다."),
    SUCCESS_SEND_EMAIL(HttpStatus.OK, "S502", "이메일 전송에 성공했습니다."),
    SUCCESS_SEND_SMS(HttpStatus.OK, "S503", "SMS 전송에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
