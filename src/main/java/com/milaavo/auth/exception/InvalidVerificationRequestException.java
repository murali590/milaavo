package com.milaavo.auth.exception;

public class InvalidVerificationRequestException extends RuntimeException {
    public InvalidVerificationRequestException(String message) {
        super(message);
    }
}
