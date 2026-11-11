package com.milaavo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Public demo login, shown on the login page. Only used by the "demo" profile.
@ConfigurationProperties(prefix = "app.demo.account")
public record DemoAccountProperties(String name, String mobileNumber, String password) {
}