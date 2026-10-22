
package com.milaavo.notification.sms;

public interface SmsSender {

    void send(String mobileNumber, String message);
}
