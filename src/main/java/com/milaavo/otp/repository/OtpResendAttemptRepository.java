package com.milaavo.otp.repository;

import com.milaavo.otp.entity.OtpResendAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface OtpResendAttemptRepository
        extends JpaRepository<OtpResendAttempt, UUID> {

    @Query("""
        SELECT COUNT(attempt)
        FROM OtpResendAttempt attempt
        WHERE attempt.user.id = :userId
          AND attempt.requestedAt > :windowStart
        """)
    long countRecentAttempts(
            @Param("userId") UUID userId,
            @Param("windowStart") Instant windowStart);

    @Query("""
        SELECT MAX(attempt.requestedAt)
        FROM OtpResendAttempt attempt
        WHERE attempt.user.id = :userId
        """)
    Instant findLatestAttemptTime(@Param("userId") UUID userId);
}