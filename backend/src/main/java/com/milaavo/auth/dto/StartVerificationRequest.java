package com.milaavo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartVerificationRequest {

    @NotBlank(message = "Mobile number is required")
    @Size(max = 20, message = "Mobile number must be at most 20 characters")
    private String mobileNumber;

    @ToString.Exclude
    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must be at most 72 characters")
    private String password;
}