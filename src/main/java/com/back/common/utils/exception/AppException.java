package com.back.common.utils.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String field;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.field = null;
    }

    public AppException(ErrorCode errorCode, String field) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.field = field;
    }

    public AppException(ErrorCode errorCode, String field, String message) {
        super(message);
        this.errorCode = errorCode;
        this.field = field;
    }
}
