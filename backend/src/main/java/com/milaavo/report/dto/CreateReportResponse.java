package com.milaavo.report.dto;

import java.time.Instant;
import java.util.UUID;

import com.milaavo.report.enums.ReportType;

public record CreateReportResponse(
        UUID reportId,
        ReportType type,
        String itemName,
        String description,
        String city,
        String status,
        Instant createdAt
) {
}