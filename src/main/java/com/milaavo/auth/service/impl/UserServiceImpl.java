package com.milaavo.auth.service.impl;

import com.milaavo.auth.dto.LoginRequest;
import com.milaavo.auth.dto.LoginResponse;
import com.milaavo.auth.dto.RegisterRequest;
import com.milaavo.auth.entity.User;
import com.milaavo.auth.enums.UserStatus;
import com.milaavo.auth.repository.UserRepository;
import com.milaavo.auth.service.JwtService;
import com.milaavo.auth.service.UserService;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.milaavo.auth.dto.RegisterResponse;
import com.milaavo.auth.dto.VerifyMobileOtpRequest;
import com.milaavo.exception.DuplicateMobileNumberException;
import com.milaavo.otp.config.OtpResendProperties;
import com.milaavo.otp.entity.OtpResendAttempt;
import com.milaavo.otp.enums.OtpPurpose;
import com.milaavo.otp.repository.OtpResendAttemptRepository;
import com.milaavo.otp.service.OtpService;

import com.milaavo.auth.dto.ResentVerificationOTPRequest;
import com.milaavo.auth.dto.ResentVerificationOTPResponse;
import com.milaavo.auth.exception.InvalidVerificationRequestException;
import com.milaavo.auth.exception.UserAlreayActiveException;
import com.milaavo.otp.exception.OtpResendRateLimitException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final OtpService otpService;
        private final OtpResendAttemptRepository otpResendAttemptRepository;
        private final OtpResendProperties otpResendProperties;

        @Override
        @Transactional
        public RegisterResponse register(RegisterRequest request) {
                String mobileNumber = request.getMobileNumber().trim();

                if (userRepository.existsByMobileNumber(mobileNumber)) {
                        throw new DuplicateMobileNumberException(
                                        "An account with this mobile number already exists");
                }

                User user = new User();
                user.setName(request.getName().trim());
                user.setMobileNumber(mobileNumber);
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                user.setMobileVerified(false);
                user.setStatus(UserStatus.PENDING_VERIFICATION);

                User savedUser = userRepository.save(user);

                UUID challengeId = otpService.issueOtp(
                                savedUser.getId(),
                                OtpPurpose.MOBILE_VERIFICATION,
                                savedUser.getMobileNumber());

                return RegisterResponse.builder()
                                .userId(savedUser.getId())
                                .name(savedUser.getName())
                                .mobileNumber(savedUser.getMobileNumber())
                                .mobileVerified(savedUser.isMobileVerified())
                                .status(savedUser.getStatus())
                                .createdAt(savedUser.getCreatedAt())
                                .challengeId(challengeId)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public LoginResponse login(LoginRequest request) {
                String mobileNumber = request.getMobileNumber().trim();

                User user = userRepository.findByMobileNumber(mobileNumber)
                                .orElseThrow(() -> new BadCredentialsException(
                                                "Invalid mobile number or password"));

                if (!passwordEncoder.matches(
                                request.getPassword(), user.getPasswordHash())) {
                        throw new BadCredentialsException(
                                        "Invalid mobile number or password");
                }

                if (user.getStatus() != UserStatus.ACTIVE
                                || !user.isMobileVerified()) {
                        throw new AccessDeniedException(
                                        "Account is not active or mobile is not verified");
                }

                return LoginResponse.builder()
                                .userId(user.getId())
                                .name(user.getName())
                                .mobileNumber(user.getMobileNumber())
                                .mobileVerified(user.isMobileVerified())
                                .status(user.getStatus().name())
                                .accessToken(jwtService.generateAccessToken(user.getId()))
                                .tokenType("Bearer")
                                .expiresIn(jwtService.getExpirySeconds())
                                .build();
        }

        @Override
        @Transactional
        public void verifyMobileOtp(VerifyMobileOtpRequest request) {
                User user = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new InvalidVerificationRequestException(
                                                "Invalid verification request"));

                if (user.isMobileVerified()
                                || user.getStatus() != UserStatus.PENDING_VERIFICATION) {
                        throw new UserAlreayActiveException(
                                        "Account is already verified or cannot be verified");
                }

                otpService.verifyOtp(
                                user.getId(),
                                request.getChallengeId(),
                                OtpPurpose.MOBILE_VERIFICATION,
                                request.getOtp());

                user.setMobileVerified(true);
                user.setStatus(UserStatus.ACTIVE);
        }

        @Override
        @Transactional
        public ResentVerificationOTPResponse resendVerificationOtp(
                        ResentVerificationOTPRequest request) {

                User user = userRepository.findByIdForUpdate(request.getUserId())
                                .orElseThrow(() -> new InvalidVerificationRequestException(
                                                "Invalid verification request"));

                if (user.isMobileVerified()
                                || user.getStatus() != UserStatus.PENDING_VERIFICATION) {
                        throw new UserAlreayActiveException(
                                        "Account is already verified or cannot be verified");
                }

                Instant now = Instant.now();

                Instant latestAttempt = otpResendAttemptRepository.findLatestAttemptTime(user.getId());

                if (latestAttempt != null
                                && latestAttempt.plusSeconds(
                                                otpResendProperties.cooldownSeconds()).isAfter(now)) {
                        throw new OtpResendRateLimitException(
                                        "Please wait before requesting another OTP");
                }

                Instant windowStart = now.minus(
                                otpResendProperties.windowMinutes(), ChronoUnit.MINUTES);

                long recentAttempts = otpResendAttemptRepository.countRecentAttempts(
                                user.getId(), windowStart);

                if (recentAttempts >= otpResendProperties.maxResends()) {
                        throw new OtpResendRateLimitException(
                                        "OTP resend limit exceeded. Please try again later");
                }

                OtpResendAttempt attempt = new OtpResendAttempt();
                attempt.setUser(user);
                attempt.setRequestedAt(now);
                otpResendAttemptRepository.save(attempt);

                UUID challengeId = otpService.issueOtp(
                                user.getId(),
                                OtpPurpose.MOBILE_VERIFICATION,
                                user.getMobileNumber());

                return ResentVerificationOTPResponse.builder()
                                .challengeId(challengeId)
                                .build();
        }

}
