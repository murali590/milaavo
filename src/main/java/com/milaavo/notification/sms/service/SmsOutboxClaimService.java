package com.milaavo.notification.sms.service;

import com.milaavo.notification.sms.entity.SmsOutbox;
import com.milaavo.notification.sms.enums.SmsOutboxStatus;
import com.milaavo.notification.sms.repository.SmsOutboxRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmsOutboxClaimService {

    private final SmsOutboxRepository smsOutboxRepository;

    @Transactional
    public List<UUID> claimPendingMessages() {
        List<SmsOutbox> messages =
                smsOutboxRepository.claimPendingMessages(Instant.now());

        for (SmsOutbox message : messages) {
            message.setStatus(SmsOutboxStatus.PROCESSING);
            message.setProcessingStartedAt(Instant.now());
        }

        smsOutboxRepository.saveAll(messages);

        return messages.stream()
                .map(SmsOutbox::getId)
                .toList();
    }
}