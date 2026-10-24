package com.milaavo.otp.enums;

public enum OtpVerificationResult {
    VERIFIED,
    INVALID,
    EXPIRED,
    ALREADY_USED,
    MAX_ATTEMPTS_EXCEEDED,
    CHALLENGE_NOT_FOUND,
    SUPERSEDED
}