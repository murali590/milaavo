package com.milaavo.auth.controller;

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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

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

        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
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
