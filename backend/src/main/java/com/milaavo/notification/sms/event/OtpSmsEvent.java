package com.milaavo.notification.sms.event;

public record OtpSmsEvent(
        String mobileNumber,
        String message
) {}