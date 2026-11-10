package com.milaavo.auth.repository;

import com.milaavo.auth.entity.RefreshToken;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // Lock the row so two refresh calls with the same token can't both rotate it.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RefreshToken t WHERE t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken t
            SET t.revokedAt = :now
            WHERE t.familyId = :familyId
              AND t.revokedAt IS NULL
            """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);
}