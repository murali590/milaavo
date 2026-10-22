
package com.milaavo.notification.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalSmsSender implements SmsSender {

    private static final Logger log =
            LoggerFactory.getLogger(LocalSmsSender.class);

    @Override
    public void send(String mobileNumber, String message) {
        log.info("LOCAL SMS to {}: {}", mobileNumber, message);
    }
}
