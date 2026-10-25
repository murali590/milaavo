package com.milaavo.notification.sms.dto;

import java.util.UUID;

public record ClaimedMessage(UUID messageId, UUID claimToken) {
}