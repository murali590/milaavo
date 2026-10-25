package com.milaavo.notification.sms.service;

import com.milaavo.notification.sms.entity.SmsOutbox;
import com.milaavo.notification.sms.enums.SmsOutboxStatus;
import com.milaavo.notification.sms.repository.SmsOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SmsOutboxService {

    private final SmsOutboxRepository smsOutboxRepository;
    private final SmsMessageCipher smsMessageCipher;

    @Transactional
    public void enqueue(String mobileNumber, String message, Instant expiresAt) {
        String encryptedMessage = smsMessageCipher.encrypt(message);

        SmsOutbox smsOutbox = SmsOutbox.builder()
                .mobileNumber(mobileNumber)
                .message(encryptedMessage)
                .createdAt(Instant.now())
                .nextAttemptAt(Instant.now())
                .status(SmsOutboxStatus.PENDING)
                .attemptCount(0)
                .expiresAt(expiresAt)
                .build();

        smsOutboxRepository.save(smsOutbox);
    }
}