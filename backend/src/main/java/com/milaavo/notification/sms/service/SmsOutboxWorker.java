package com.milaavo.notification.sms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.milaavo.notification.sms.dto.ClaimedMessage;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsOutboxWorker {

    private final SmsOutboxClaimService claimService;
    private final SmsOutboxProcessor processor;
    private final SmsOutboxRecoveryService recoveryService;

    @Scheduled(fixedDelayString = "${app.sms-outbox.poll-delay-ms:5000}")
    public void processPendingMessages() {
        for (ClaimedMessage claimed : claimService.claimPendingMessages()) {
            try {
                processor.process(claimed);
            } catch (Exception ex) {
                log.error("Unexpected error processing SMS outbox message {}", claimed.messageId(), ex);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.sms-outbox.recovery-delay-ms:30000}")
    public void recoverStaleMessages() {
        recoveryService.recoverStaleMessages();
    }
}