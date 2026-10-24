package com.milaavo.notification.sms.service;

import com.milaavo.notification.sms.repository.SmsOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsOutboxRecoveryService {

    private final SmsOutboxRepository smsOutboxRepository;

    @Transactional
    public void recoverStaleMessages() {
        Instant cutoff = Instant.now().minus(2, ChronoUnit.MINUTES);

        int recovered =
                smsOutboxRepository.recoverStaleProcessingMessages(cutoff, SmsOutboxStateService.MAX_ATTEMPTS);

        if (recovered > 0) {
            log.warn("Recovered {} stale SMS outbox messages", recovered);
        }
    }
}