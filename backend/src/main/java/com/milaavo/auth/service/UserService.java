package com.milaavo.auth.service;

import com.milaavo.auth.dto.AuthSession;
import com.milaavo.auth.dto.LoginRequest;
import com.milaavo.auth.dto.RegisterRequest;
import com.milaavo.auth.dto.RegisterResponse;
import com.milaavo.auth.dto.ResentVerificationOTPRequest;
import com.milaavo.auth.dto.ResentVerificationOTPResponse;
import com.milaavo.auth.dto.StartVerificationRequest;
import com.milaavo.auth.dto.StartVerificationResponse;
import com.milaavo.auth.dto.VerifyMobileOtpRequest;

public interface UserService {

    RegisterResponse register(RegisterRequest request);

    AuthSession login(LoginRequest request);

    AuthSession refresh(String rawRefreshToken);

    void verifyMobileOtp(VerifyMobileOtpRequest request);

    ResentVerificationOTPResponse resendVerificationOtp(ResentVerificationOTPRequest request);

    StartVerificationResponse startVerification(StartVerificationRequest request);
}
