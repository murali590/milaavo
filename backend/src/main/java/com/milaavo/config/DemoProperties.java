package com.milaavo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Demo-only switches. Everything defaults to off.
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(boolean showOtp) {
}