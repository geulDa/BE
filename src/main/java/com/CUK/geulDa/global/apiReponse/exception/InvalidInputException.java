package com.CUK.geulDa.global.apiReponse.exception;

import com.CUK.geulDa.global.apiReponse.code.ErrorCode;
import com.CUK.geulDa.global.apiReponse.exception.BusinessException;

public class InvalidInputException extends BusinessException {

    public InvalidInputException() {
        super(ErrorCode.INVALID_INPUT);
    }

    public InvalidInputException(String message) {
        super(ErrorCode.INVALID_INPUT, message);
    }

    public InvalidInputException(String fieldName, String reason) {
        super(ErrorCode.INVALID_INPUT, fieldName + " 필드가 유효하지 않습니다: " + reason);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(ErrorCode.INVALID_INPUT, message, cause);
    }
}
