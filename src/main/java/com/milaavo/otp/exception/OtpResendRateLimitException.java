package com.milaavo.otp.exception;

public class OtpResendRateLimitException extends RuntimeException {
    public OtpResendRateLimitException(String message) {
        super(message);
    }
}
