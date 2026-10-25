package com.milaavo.otp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.otp.resend")
public record OtpResendProperties(
        long cooldownSeconds,
        int maxResends,
        long windowMinutes
) {
}
