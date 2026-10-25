package com.milaavo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("app.cors.allowed-origins must list at least one origin");
        }
        if (allowedOrigins.contains("*")) {
            throw new IllegalArgumentException("app.cors.allowed-origins must not contain '*'. List exact origins");
        }
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}