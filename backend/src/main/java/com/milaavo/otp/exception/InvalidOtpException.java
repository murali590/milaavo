package com.milaavo.otp.exception;

public class InvalidOtpException extends RuntimeException {

    private final String errorCode;

    public InvalidOtpException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}