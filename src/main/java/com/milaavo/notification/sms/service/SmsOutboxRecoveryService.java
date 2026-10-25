package com.milaavo.notification.sms.service;

import com.milaavo.notification.sms.repository.SmsOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.milaavo.notification.sms.config.SmsOutboxProperties;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsOutboxRecoveryService {

    private final SmsOutboxRepository smsOutboxRepository;
    private final SmsOutboxProperties properties;

    @Transactional
    public void recoverStaleMessages() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(properties.lease());

        int recovered = smsOutboxRepository.recoverStaleProcessingMessages(cutoff, SmsOutboxStateService.MAX_ATTEMPTS);

        if (recovered > 0) {
            log.warn("Recovered {} stale SMS outbox messages", recovered);
        }

        int expired = smsOutboxRepository.failExpiredMessages(now);
        if (expired > 0) {
            log.warn("Marked {} expired SMS outbox messages as FAILED", expired);
        }
    }
}