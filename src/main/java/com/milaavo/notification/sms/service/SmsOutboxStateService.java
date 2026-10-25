package com.milaavo.notification.sms.service;

import com.milaavo.notification.sms.dto.ClaimedMessage;
import com.milaavo.notification.sms.entity.SmsOutbox;
import com.milaavo.notification.sms.enums.SmsOutboxStatus;
import com.milaavo.notification.sms.repository.SmsOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsOutboxStateService {

    public static final int MAX_ATTEMPTS = 5;
    private static final int MAX_ERROR_LENGTH = 500;

    private final SmsOutboxRepository smsOutboxRepository;

    @Transactional(readOnly = true)
    public Optional<SmsOutbox> loadForSending(ClaimedMessage claimed) {
        return smsOutboxRepository.findById(claimed.messageId())
                .filter(message -> message.getStatus() == SmsOutboxStatus.PROCESSING)
                .filter(message -> claimed.claimToken().equals(message.getClaimToken()));
    }

    @Transactional
    public void markSent(ClaimedMessage claimed) {
        findOwned(claimed).ifPresentOrElse(message -> {
            message.setStatus(SmsOutboxStatus.SENT);
            message.setSentAt(Instant.now());
            message.setLastError(null);
            message.setProcessingStartedAt(null);
            message.setClaimToken(null);
        }, () -> logLeaseLost(claimed, "SENT"));
    }

    @Transactional
    public void recordFailure(ClaimedMessage claimed, Exception failure) {
        findOwned(claimed).ifPresentOrElse(message -> {
            message.setLastError(truncate(failure.getMessage()));
            message.setProcessingStartedAt(null);
            message.setClaimToken(null);

            if (message.getAttemptCount() >= MAX_ATTEMPTS) {
                message.setStatus(SmsOutboxStatus.FAILED);
            } else {
                message.setStatus(SmsOutboxStatus.PENDING);
                long retryDelaySeconds = 1L << message.getAttemptCount();
                message.setNextAttemptAt(Instant.now().plusSeconds(retryDelaySeconds));
            }

            log.warn("SMS delivery failed for outbox ID {}. Attempt {} of {}",
                    message.getId(), message.getAttemptCount(), MAX_ATTEMPTS);
        }, () -> logLeaseLost(claimed, "failure"));
    }

    private Optional<SmsOutbox> findOwned(ClaimedMessage claimed) {
        return smsOutboxRepository.findByIdAndClaimTokenAndStatus(
                claimed.messageId(), claimed.claimToken(), SmsOutboxStatus.PROCESSING);
    }

    private void logLeaseLost(ClaimedMessage claimed, String outcome) {
        log.warn("Lease lost for SMS outbox ID {}: another worker owns it now, skipping {} update",
                claimed.messageId(), outcome);
    }

    private String truncate(String error) {
        if (error == null) {
            return "SMS delivery failed";
        }
        return error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
    }
}