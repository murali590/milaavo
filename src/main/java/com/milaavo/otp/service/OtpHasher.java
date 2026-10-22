
package com.milaavo.otp.service;

import com.milaavo.otp.entity.OtpChallenge;
import com.milaavo.otp.enums.OtpPurpose;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class OtpHasher {

    private final SecretKey otpHmacKey;

    public OtpHasher(SecretKey otpHmacKey) {
        this.otpHmacKey = otpHmacKey;
    }

    public String hash(
            java.util.UUID userId,
            OtpPurpose purpose,
            String otp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(otpHmacKey);

            String value = userId + ":" + purpose + ":" + otp;
            byte[] hash = mac.doFinal(
                    value.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to securely hash OTP", exception);
        }
    }

    public boolean matches(OtpChallenge challenge, String candidateOtp) {
        byte[] expected = Base64.getDecoder().decode(
                challenge.getOtpHash());
        byte[] actual = Base64.getDecoder().decode(
                hash(challenge.getUser().getId(),
                        challenge.getPurpose(), candidateOtp));

        return MessageDigest.isEqual(expected, actual);
    }
}
