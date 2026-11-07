package com.milaavo.report.controller;

import com.milaavo.common.security.AuthUser;
import com.milaavo.common.security.CurrentUser;
import com.milaavo.report.dto.CreateReportRequest;
import com.milaavo.report.dto.CreateReportResponse;
import com.milaavo.report.dto.PagedResponse;
import com.milaavo.report.enums.ReportType;
import com.milaavo.report.service.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    public ResponseEntity<CreateReportResponse> createReport(
            @CurrentUser AuthUser currentUser,
            @Valid @RequestBody CreateReportRequest request) {

        CreateReportResponse response = reportService.createReport(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<CreateReportResponse>> getAllReports(
            @CurrentUser AuthUser currentUser,
            @RequestParam(required = false) ReportType type,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number must be at least 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1") @Max(value = 100, message = "Size must be at most 100") int size) {

        return ResponseEntity.ok(reportService.getAllReports(currentUser, type, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreateReportResponse> getReportById(
            @CurrentUser AuthUser currentUser,
            @PathVariable UUID id) {

        return ResponseEntity.ok(reportService.getReportById(currentUser, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreateReportResponse> updateReport(
            @CurrentUser AuthUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody CreateReportRequest request) {

        return ResponseEntity.ok(reportService.updateReport(currentUser, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(
            @CurrentUser AuthUser currentUser,
            @PathVariable UUID id) {

        reportService.deleteReport(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}