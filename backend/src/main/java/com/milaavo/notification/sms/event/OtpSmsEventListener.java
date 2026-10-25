package com.milaavo.notification.sms.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.milaavo.notification.sms.service.SmsSender;

@Component
public class OtpSmsEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(OtpSmsEventListener.class);

    private final SmsSender smsSender;

    public OtpSmsEventListener(SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOtpSmsEvent(OtpSmsEvent event) {
        try {
            smsSender.send(event.mobileNumber(), event.message());
        } catch (Exception ex) {
            log.error("Failed to send OTP SMS after transaction commit");
        }
    }
}