package com.milaavo.notification.sms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.sms-outbox")
public record SmsOutboxProperties(Duration lease) {

    public SmsOutboxProperties {
        if (lease == null || lease.isZero() || lease.isNegative()) {
            throw new IllegalArgumentException(
                    "app.sms-outbox.lease must be a positive duration, for example 2m");
        }
    }
}