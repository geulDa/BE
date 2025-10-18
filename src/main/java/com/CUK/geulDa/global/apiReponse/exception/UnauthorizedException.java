package com.CUK.geulDa.global.apiReponse.exception;

import com.CUK.geulDa.global.apiReponse.code.ErrorCode;
import com.CUK.geulDa.global.apiReponse.exception.BusinessException;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED, message, cause);
    }
}
