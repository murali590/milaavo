package com.milaavo.notification.sms.service;

import com.milaavo.notification.sms.dto.ClaimedMessage;
import com.milaavo.notification.sms.entity.SmsOutbox;
import com.milaavo.notification.sms.enums.SmsOutboxStatus;
import com.milaavo.notification.sms.repository.SmsOutboxRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmsOutboxClaimService {

    private final SmsOutboxRepository smsOutboxRepository;

    @Transactional
    public List<ClaimedMessage> claimPendingMessages() {
        List<SmsOutbox> messages = smsOutboxRepository.claimPendingMessages(Instant.now());
        Instant now = Instant.now();

        List<ClaimedMessage> claimed = new ArrayList<>();
        for (SmsOutbox message : messages) {
            UUID token = UUID.randomUUID();
            message.setStatus(SmsOutboxStatus.PROCESSING);
            message.setProcessingStartedAt(now);
            message.setAttemptCount(message.getAttemptCount() + 1);
            message.setClaimToken(token);
            claimed.add(new ClaimedMessage(message.getId(), token));
        }
        return claimed;
    }
}