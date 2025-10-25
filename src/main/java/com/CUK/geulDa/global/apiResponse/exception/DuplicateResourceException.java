package com.CUK.geulDa.global.apiResponse.exception;

import com.CUK.geulDa.global.apiResponse.code.ErrorCode;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException() {
        super(ErrorCode.DUPLICATE_RESOURCE);
    }

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }

    public DuplicateResourceException(String resourceName, String value) {
        super(ErrorCode.DUPLICATE_RESOURCE, resourceName + " '" + value + "'은(는) 이미 사용 중입니다.");
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(ErrorCode.DUPLICATE_RESOURCE, message, cause);
    }
}
