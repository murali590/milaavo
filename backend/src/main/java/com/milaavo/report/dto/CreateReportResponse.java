package com.milaavo.report.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateReportResponse(
        UUID reportId,
        String itemName,
        String description,
        String city,
        String status,
        Instant createdAt
) {
}