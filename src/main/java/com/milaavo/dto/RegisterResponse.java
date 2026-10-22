
package com.milaavo.dto;

import com.milaavo.enums.UserStatus;
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
}
