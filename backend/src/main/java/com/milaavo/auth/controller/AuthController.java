package com.milaavo.auth.controller;

import com.milaavo.auth.dto.AuthSession;
import com.milaavo.auth.dto.LoginRequest;
import com.milaavo.auth.dto.LoginResponse;
import com.milaavo.auth.dto.RegisterRequest;
import com.milaavo.auth.dto.RegisterResponse;
import com.milaavo.auth.dto.VerifyMobileOtpRequest;
import com.milaavo.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.milaavo.auth.dto.ResentVerificationOTPRequest;
import com.milaavo.auth.dto.ResentVerificationOTPResponse;
import com.milaavo.auth.dto.StartVerificationRequest;
import com.milaavo.auth.dto.StartVerificationResponse;

import com.milaavo.auth.dto.AuthSession;
import com.milaavo.auth.service.RefreshTokenService;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookie refreshTokenCookie;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthSession session = userService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(session.refreshToken()))
                .body(session.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {

        AuthSession session = userService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(session.refreshToken()))
                .body(session.response());
    }

    // Public on purpose: the access token may already be expired when the user logs out.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {

        refreshTokenService.revoke(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear())
                .build();
    }

    @PostMapping("/verify-mobile")
    public ResponseEntity<Void> verifyMobile(
            @Valid @RequestBody VerifyMobileOtpRequest request) {

        userService.verifyMobileOtp(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify/resend-otp")
    public ResponseEntity<ResentVerificationOTPResponse> resentVerificationOtp(
            @Valid @RequestBody ResentVerificationOTPRequest request) {

        ResentVerificationOTPResponse response = userService.resendVerificationOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify/start")
    public ResponseEntity<StartVerificationResponse> startVerification(
            @Valid @RequestBody StartVerificationRequest request) {

        return ResponseEntity.ok(userService.startVerification(request));
    }
}
