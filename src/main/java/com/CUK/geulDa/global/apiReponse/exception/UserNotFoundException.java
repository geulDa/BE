package com.CUK.geulDa.global.apiReponse.exception;

import com.CUK.geulDa.global.apiReponse.code.ErrorCode;
import com.CUK.geulDa.global.apiReponse.exception.BusinessException;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }

    public UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }

    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, "ID가 " + userId + "인 사용자를 찾을 수 없습니다.");
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(ErrorCode.USER_NOT_FOUND, message, cause);
    }
}
