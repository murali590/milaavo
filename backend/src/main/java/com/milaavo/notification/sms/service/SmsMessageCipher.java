package com.milaavo.notification.sms.service;

import com.milaavo.common.security.crypto.AesGcmCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// SMS outbox messages are encrypted with their own key (key separation).
@Component
public class SmsMessageCipher {

    private final AesGcmCipher cipher;

    public SmsMessageCipher(@Value("${app.sms-outbox.encryption-key}") String base64Key) {
        this.cipher = AesGcmCipher.fromBase64Key(base64Key, "app.sms-outbox.encryption-key");
    }

    public String encrypt(String plaintext) {
        return cipher.encrypt(plaintext);
    }

    public String decrypt(String encoded) {
        return cipher.decrypt(encoded);
    }
}