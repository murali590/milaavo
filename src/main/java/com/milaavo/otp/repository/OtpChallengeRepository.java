
package com.milaavo.otp.repository;

import com.milaavo.otp.entity.OtpChallenge;
import com.milaavo.otp.enums.OtpPurpose;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.UUID;

public interface OtpChallengeRepository
                extends JpaRepository<OtpChallenge, UUID> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        SELECT challenge
                        FROM OtpChallenge challenge
                        WHERE challenge.id = :challengeId
                          AND challenge.user.id = :userId
                          AND challenge.purpose = :purpose
                        """)
        Optional<OtpChallenge> findForVerification(
                        @Param("challengeId") UUID challengeId,
                        @Param("userId") UUID userId,
                        @Param("purpose") OtpPurpose purpose);

        @Modifying
        @Query("""
                        UPDATE OtpChallenge challenge
                        SET challenge.failedAttempts = challenge.failedAttempts + 1
                        WHERE challenge.id = :challengeId
                          AND challenge.consumedAt IS NULL
                          AND challenge.failedAttempts < :maxAttempts
                        """)
        int incrementFailedAttempts(
                        @Param("challengeId") UUID challengeId,
                        @Param("maxAttempts") int maxAttempts);
}
