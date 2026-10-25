
package com.milaavo.notification.sms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LocalSmsSender.class);

    @Override
    public void send(String mobileNumber, String message) {
        if (System.getenv("SLOW_SMS") != null) {
            try {
                Thread.sleep(30_000); // TEMP: slow provider on this instance only
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("LOCAL SMS to {}: {}", mobileNumber, message);
    }
}
