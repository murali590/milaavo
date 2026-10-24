
package com.milaavo.notification.sms.service;

public interface SmsSender {

    void send(String mobileNumber, String message);
}
