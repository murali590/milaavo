package com.milaavo.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(Duration ttl, boolean cookieSecure) {

    public RefreshTokenProperties {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("app.refresh-token.ttl must be positive");
        }
    }
}