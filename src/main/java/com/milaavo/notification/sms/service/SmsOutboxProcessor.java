package com.milaavo.notification.sms.service;

import com.milaavo.notification.sms.dto.ClaimedMessage;
import com.milaavo.notification.sms.entity.SmsOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmsOutboxProcessor {

        private final SmsOutboxStateService stateService;
        private final SmsSender smsSender;

        // Not @Transactional on purpose: no DB connection is held during the provider
        // call.
        public void process(ClaimedMessage claimed) {
                Optional<SmsOutbox> loaded = stateService.loadForSending(claimed);
                if (loaded.isEmpty()) {
                        return;
                }

                SmsOutbox message = loaded.get();

                try {
                        smsSender.send(message.getMobileNumber(), message.getMessage());
                } catch (Exception ex) {
                        stateService.recordFailure(claimed, ex);
                        return;
                }

                stateService.markSent(claimed);
        }
}