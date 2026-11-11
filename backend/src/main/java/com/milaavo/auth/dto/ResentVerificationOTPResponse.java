package com.milaavo.auth.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResentVerificationOTPResponse {
    private UUID challengeId;
    // Only in demo mode (app.demo.show-otp=true). Left out of the JSON otherwise.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String demoOtp;
}
