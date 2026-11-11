
package com.milaavo.otp.service.impl;

import com.milaavo.auth.entity.User;
import com.milaavo.auth.repository.UserRepository;
import com.milaavo.config.DemoProperties;
import com.milaavo.notification.sms.event.OtpSmsEvent;
import com.milaavo.notification.sms.service.SmsOutboxService;
import com.milaavo.notification.sms.service.SmsSender;
import com.milaavo.otp.entity.OtpChallenge;
import com.milaavo.otp.enums.OtpPurpose;
import com.milaavo.otp.enums.OtpVerificationResult;
import com.milaavo.otp.exception.InvalidOtpException;
import com.milaavo.otp.repository.OtpChallengeRepository;
import jakarta.transaction.Transactional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.milaavo.otp.service.OtpService;
import com.milaavo.otp.service.OtpHasher;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.milaavo.otp.service.IssuedOtp;

@Service
public class OtpServiceImpl implements OtpService {

    private static final int OTP_VALIDITY_MINUTES = 5;

    private final OtpVerificationTransaction otpVerificationTransaction;
    private final OtpHasher otpHasher;
    private final ApplicationEventPublisher eventPublisher;
    private final SmsOutboxService smsOutboxService;

    private final OtpChallengeRepository otpRepository;
    private final UserRepository userRepository;
    private final SmsSender smsSender;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    private final DemoProperties demoProperties;

    public OtpServiceImpl(
            OtpVerificationTransaction otpVerificationTransaction,
            OtpChallengeRepository otpRepository,
            UserRepository userRepository,
            SmsSender smsSender,
            OtpHasher otpHasher,
            ApplicationEventPublisher eventPublisher,
            SmsOutboxService smsOutboxService,
            DemoProperties demoProperties) {
        this.otpVerificationTransaction = otpVerificationTransaction;
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.smsSender = smsSender;
        this.otpHasher = otpHasher;
        this.eventPublisher = eventPublisher;
        this.smsOutboxService = smsOutboxService;
        this.demoProperties = demoProperties;
        if (demoProperties.showOtp()) {
            log.warn("DEMO MODE: OTPs are returned in API responses. Never enable this with real users");
        }
    }

    @Override
    @Transactional
    public IssuedOtp issueOtp(
            UUID userId,
            OtpPurpose purpose,
            String mobileNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found"));

        Instant now = Instant.now();

        otpRepository.invalidateActiveChallenges(
                userId,
                purpose,
                now);

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

        smsOutboxService.enqueue(mobileNumber, "Your Milaavo verification code is " + otp
                + ". It expires in 5 minutes.", challenge.getExpiresAt());

        String demoOtp = demoProperties.showOtp() ? otp : null;
        return new IssuedOtp(savedChallenge.getId(), demoOtp);

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
                throw new InvalidOtpException("OTP_EXPIRED", "OTP has expired");
            case ALREADY_USED ->
                throw new InvalidOtpException("OTP_ALREADY_USED", "OTP has already been used");
            case MAX_ATTEMPTS_EXCEEDED ->
                throw new InvalidOtpException("MAX_ATTEMPTS_EXCEEDED", "Maximum OTP attempts exceeded");
            case INVALID, CHALLENGE_NOT_FOUND ->
                throw new InvalidOtpException("INVALID_OTP", "Invalid OTP");
            case SUPERSEDED ->
                throw new InvalidOtpException("SUPERSEDED",
                        "This OTP is no longer valid. Please use the latest OTP sent to your mobile");
            default ->
                throw new IllegalStateException("Unexpected OTP verification result: " + result);
        }
    }
}
