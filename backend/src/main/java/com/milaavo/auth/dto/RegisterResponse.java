
package com.milaavo.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.milaavo.auth.enums.UserStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private UUID userId;
    private String name;
    private String mobileNumber;
    private boolean mobileVerified;
    private UserStatus status;
    private Instant createdAt;
    private UUID challengeId;
        // Only in demo mode (app.demo.show-otp=true). Left out of the JSON otherwise.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String demoOtp;
}
