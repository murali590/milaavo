package com.milaavo.notification.sms.service;

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

    public  static final int MAX_ATTEMPTS = 5;
    private static final int MAX_ERROR_LENGTH = 500;

    private final SmsOutboxRepository smsOutboxRepository;

    @Transactional(readOnly = true)
    public Optional<SmsOutbox> loadForSending(UUID messageId) {
        return smsOutboxRepository.findById(messageId)
                .filter(message -> message.getStatus() == SmsOutboxStatus.PROCESSING);
    }

    @Transactional
    public void markSent(UUID messageId) {
        smsOutboxRepository.findById(messageId).ifPresent(message -> {
            message.setStatus(SmsOutboxStatus.SENT);
            message.setSentAt(Instant.now());
            message.setLastError(null);
            message.setProcessingStartedAt(null);
        });
    }

    @Transactional
    public void recordFailure(UUID messageId, Exception failure) {
        smsOutboxRepository.findById(messageId).ifPresent(message -> {
            message.setLastError(truncate(failure.getMessage()));
            message.setProcessingStartedAt(null);

            if (message.getAttemptCount() >= MAX_ATTEMPTS) {
                message.setStatus(SmsOutboxStatus.FAILED);
            } else {
                message.setStatus(SmsOutboxStatus.PENDING);
                long retryDelaySeconds = 1L << message.getAttemptCount();
                message.setNextAttemptAt(Instant.now().plusSeconds(retryDelaySeconds));
            }

            log.warn("SMS delivery failed for outbox ID {}. Attempt {} of {}",
                    message.getId(), message.getAttemptCount(), MAX_ATTEMPTS);
        });
    }

    private String truncate(String error) {
        if (error == null) {
            return "SMS delivery failed";
        }
        return error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
    }
}