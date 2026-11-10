package com.milaavo.auth.dto;

// What the service gives the controller: the JSON body and the raw refresh token for the cookie.
public record AuthSession(LoginResponse response, String refreshToken) {
}