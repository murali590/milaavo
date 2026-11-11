package com.milaavo.otp.service;

import java.util.UUID;

// demoOtp is null unless app.demo.show-otp is on.
public record IssuedOtp(UUID challengeId, String demoOtp) {
}