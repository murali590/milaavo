package com.milaavo.auth.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartVerificationResponse {

    private UUID userId;
    private UUID challengeId;
    private String mobileNumber;
}