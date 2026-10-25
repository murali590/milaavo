package com.milaavo.auth.exception;

public class UserAlreayActiveException extends RuntimeException {

    public UserAlreayActiveException(String message) {
        super(message);
    }

}
