
package com.milaavo.otp.service.impl;

import com.milaavo.otp.entity.OtpChallenge;
import com.milaavo.otp.enums.OtpPurpose;
import com.milaavo.otp.enums.OtpVerificationResult;
import com.milaavo.otp.repository.OtpChallengeRepository;
import com.milaavo.otp.service.OtpHasher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OtpVerificationTransaction {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final OtpChallengeRepository otpRepository;

    private final OtpHasher otpHasher;

    public OtpVerificationTransaction(
            OtpChallengeRepository otpRepository,
            OtpHasher otpHasher) {
        this.otpRepository = otpRepository;
        this.otpHasher = otpHasher;
    }

    @Transactional
    public OtpVerificationResult verify(
            UUID userId,
            UUID challengeId,
            OtpPurpose purpose,
            String otp) {

        OtpChallenge challenge = otpRepository
                .findForVerification(challengeId, userId, purpose)
                .orElse(null);

        if (challenge == null) {
            return OtpVerificationResult.CHALLENGE_NOT_FOUND;
        }

        if (challenge.getConsumedAt() != null) {
            return OtpVerificationResult.ALREADY_USED;
        }

        if (!Instant.now().isBefore(challenge.getExpiresAt())) {
            return OtpVerificationResult.EXPIRED;
        }

        if (challenge.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            return OtpVerificationResult.MAX_ATTEMPTS_EXCEEDED;
        }

        if (otp == null || !otp.matches("\\d{6}")
                || !otpHasher.matches(challenge, otp)) {
            incrementFailedAttempts(challenge);
            return OtpVerificationResult.INVALID;
        }

        challenge.setConsumedAt(Instant.now());
        return OtpVerificationResult.VERIFIED;
    }

    private void incrementFailedAttempts(OtpChallenge challenge) {
        challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
    }
}
