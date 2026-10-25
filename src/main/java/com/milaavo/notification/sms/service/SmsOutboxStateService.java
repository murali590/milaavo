package com.milaavo.notification.sms.service;

import com.milaavo.notification.sms.config.SmsOutboxProperties;
import com.milaavo.notification.sms.dto.ClaimedMessage;
import com.milaavo.notification.sms.entity.SmsOutbox;
import com.milaavo.notification.sms.enums.SmsOutboxStatus;
import com.milaavo.notification.sms.repository.SmsOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsOutboxStateService {

    public static final int MAX_ATTEMPTS = 5;
    private static final int MAX_ERROR_LENGTH = 500;

    private final SmsOutboxRepository smsOutboxRepository;
    private final SmsOutboxProperties properties;

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
                Duration delay = retryDelay(message.getAttemptCount());
                Instant nextAttemptAt = Instant.now().plus(delay);

                if (message.getExpiresAt() != null && !nextAttemptAt.isBefore(message.getExpiresAt())) {
                    message.setStatus(SmsOutboxStatus.FAILED);
                    log.warn("Not retrying SMS outbox ID {}: next attempt would be after expiry", message.getId());
                } else {
                    message.setStatus(SmsOutboxStatus.PENDING);
                    message.setNextAttemptAt(nextAttemptAt);
                    log.warn("SMS delivery failed for outbox ID {}. Attempt {} of {}. Retrying in {} ms",
                            message.getId(), message.getAttemptCount(), MAX_ATTEMPTS, delay.toMillis());
                }
            }
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

    // Exponential backoff with a cap and "equal jitter": a random wait between
    // delay/2 and delay.
    private Duration retryDelay(int attemptCount) {
        long baseMillis = properties.retryBaseDelay().toMillis();
        long maxMillis = properties.retryMaxDelay().toMillis();

        int exponent = Math.min(attemptCount - 1, 20);
        long exponential = Math.min(maxMillis, baseMillis * (1L << exponent));

        long half = exponential / 2;
        return Duration.ofMillis(half + ThreadLocalRandom.current().nextLong(half + 1));
    }
}