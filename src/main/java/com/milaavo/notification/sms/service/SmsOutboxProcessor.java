
package com.milaavo.notification.sms.service;

import com.milaavo.notification.sms.entity.SmsOutbox;
import com.milaavo.notification.sms.enums.SmsOutboxStatus;
import com.milaavo.notification.sms.repository.SmsOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsOutboxProcessor {

    private static final int MAX_ATTEMPTS = 5;

    private final SmsOutboxRepository smsOutboxRepository;
    private final SmsSender smsSender;

    @Transactional
    public void process(UUID messageId) {
        SmsOutbox message = smsOutboxRepository.findById(messageId)
                .orElse(null);

        if (message == null
                || message.getStatus() != SmsOutboxStatus.PROCESSING) {
            return;
        }

        message.setAttemptCount(message.getAttemptCount() + 1);

        try {
            smsSender.send(
                    message.getMobileNumber(),
                    message.getMessage()
            );

            message.setStatus(SmsOutboxStatus.SENT);
            message.setSentAt(Instant.now());
            message.setLastError(null);

        } catch (Exception ex) {
            String error = ex.getMessage() == null
                    ? "SMS delivery failed"
                    : ex.getMessage();

            message.setLastError(
                    error.length() > 500
                            ? error.substring(0, 500)
                            : error
            );

            if (message.getAttemptCount() >= MAX_ATTEMPTS) {
                message.setStatus(SmsOutboxStatus.FAILED);
            } else {
                message.setStatus(SmsOutboxStatus.PENDING);

                long retryDelaySeconds =
                        1L << message.getAttemptCount();

                message.setNextAttemptAt(
                        Instant.now().plusSeconds(retryDelaySeconds)
                );
            }
           
            log.warn(
                    "SMS delivery failed for outbox ID {}. Attempt {} of {}",
                    message.getId(),
                    message.getAttemptCount(),
                    MAX_ATTEMPTS
            );
        }

        message.setProcessingStartedAt(null);
        smsOutboxRepository.save(message);
    }
}
