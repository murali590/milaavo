package com.milaavo.auth.controller;

import com.milaavo.auth.config.RefreshTokenProperties;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookie {

    public static final String NAME = "milaavo_rt";
    private static final String PATH = "/api/auth";

    private final RefreshTokenProperties properties;

    public String create(String rawToken) {
        return build(rawToken, properties.ttl());
    }

    public String clear() {
        return build("", Duration.ZERO);
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}