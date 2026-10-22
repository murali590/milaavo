package com.milaavo.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private UUID userId;
    private String name;
    private String mobileNumber;
    private boolean mobileVerified;
    private String status;
    private String accessToken;
    private String tokenType;
    private long expiresIn;
}
