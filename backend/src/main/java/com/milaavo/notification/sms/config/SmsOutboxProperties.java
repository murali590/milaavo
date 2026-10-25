package com.milaavo.notification.sms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.sms-outbox")
public record SmsOutboxProperties(
        Duration lease,
        Duration retryBaseDelay,
        Duration retryMaxDelay) {

    public SmsOutboxProperties {
        requirePositive(lease, "lease");
        requirePositive(retryBaseDelay, "retry-base-delay");
        requirePositive(retryMaxDelay, "retry-max-delay");

        if (retryMaxDelay.compareTo(retryBaseDelay) < 0) {
            throw new IllegalArgumentException(
                    "app.sms-outbox.retry-max-delay must be >= retry-base-delay");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    "app.sms-outbox." + name + " must be a positive duration");
        }
    }
}