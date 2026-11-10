package com.milaavo.auth.exception;

// One exception for missing, unknown, expired, revoked and reused tokens.
// The client must not learn which one it was.
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Session expired. Please log in again");
    }
}