
package com.milaavo.otp.service;

import com.milaavo.otp.enums.OtpPurpose;

import java.util.UUID;

public interface OtpService {

    UUID issueOtp(UUID userId, OtpPurpose purpose, String mobileNumber);

    void verifyOtp(UUID userId, UUID challengeId, OtpPurpose purpose, String otp);
}
