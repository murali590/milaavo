
package com.milaavo.otp.service.impl;

import com.milaavo.auth.entity.User;
import com.milaavo.auth.repository.UserRepository;
import com.milaavo.notification.sms.SmsSender;
import com.milaavo.otp.entity.OtpChallenge;
import com.milaavo.otp.enums.OtpPurpose;
import com.milaavo.otp.enums.OtpVerificationResult;
import com.milaavo.otp.exception.InvalidOtpException;
import com.milaavo.otp.repository.OtpChallengeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.milaavo.otp.service.OtpService;
import com.milaavo.otp.service.OtpHasher;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class OtpServiceImpl implements OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int OTP_VALIDITY_MINUTES = 5;

    private final OtpVerificationTransaction otpVerificationTransaction;
    private final OtpHasher otpHasher;

    private final OtpChallengeRepository otpRepository;
    private final UserRepository userRepository;
    private final SmsSender smsSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpServiceImpl(
            OtpVerificationTransaction otpVerificationTransaction,
            OtpChallengeRepository otpRepository,
            UserRepository userRepository,
            SmsSender smsSender,
            OtpHasher otpHasher) {
        this.otpVerificationTransaction = otpVerificationTransaction;
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.smsSender = smsSender;
        this.otpHasher = otpHasher;
    }

    @Override
    @Transactional
    public UUID issueOtp(
            UUID userId,
            OtpPurpose purpose,
            String mobileNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found"));

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000));

        OtpChallenge challenge = new OtpChallenge();
        challenge.setUser(user);
        challenge.setPurpose(purpose);
        challenge.setOtpHash(otpHasher.hash(userId, purpose, otp));
        challenge.setExpiresAt(
                Instant.now().plus(OTP_VALIDITY_MINUTES, ChronoUnit.MINUTES));

        OtpChallenge savedChallenge = otpRepository.save(challenge);

        // Send only after the database transaction commits.
        smsSender.send(
                mobileNumber,
                "Your Milaavo verification code is " + otp
                        + ". It expires in 5 minutes.");

        return savedChallenge.getId();

    }

    @Override
    public void verifyOtp(
            UUID userId,
            UUID challengeId,
            OtpPurpose purpose,
            String otp) {

        OtpVerificationResult result = otpVerificationTransaction.verify(
                userId, challengeId, purpose, otp);

        switch (result) {
            case VERIFIED -> {
                return;
            }
            case EXPIRED ->
                throw new InvalidOtpException("OTP has expired");
            case ALREADY_USED ->
                throw new InvalidOtpException("OTP has already been used");
            case MAX_ATTEMPTS_EXCEEDED ->
                throw new InvalidOtpException("Maximum OTP attempts exceeded");
            case INVALID, CHALLENGE_NOT_FOUND ->
                throw new InvalidOtpException("Invalid OTP");
        }
    }
}
