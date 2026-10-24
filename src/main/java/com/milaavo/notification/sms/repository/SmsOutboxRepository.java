package com.milaavo.notification.sms.repository;

import com.milaavo.notification.sms.entity.SmsOutbox;
import com.milaavo.notification.sms.enums.SmsOutboxStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SmsOutboxRepository
        extends JpaRepository<SmsOutbox, UUID> {

    List<SmsOutbox> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            SmsOutboxStatus status,
            Instant now);

    @Query(value = """
            SELECT *
            FROM sms_outbox
            WHERE status = 'PENDING'
              AND next_attempt_at <= :now
            ORDER BY created_at ASC
            LIMIT 50
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<SmsOutbox> claimPendingMessages(@Param("now") Instant now);

    @Modifying
    @Query(value = """
            UPDATE sms_outbox
            SET status = 'PENDING',
                next_attempt_at = CURRENT_TIMESTAMP,
                processing_started_at = NULL,
                last_error = 'Recovered stale processing message'
            WHERE status = 'PROCESSING'
              AND processing_started_at < :cutoff
            """, nativeQuery = true)
    int recoverStaleProcessingMessages(
            @Param("cutoff") Instant cutoff);
}