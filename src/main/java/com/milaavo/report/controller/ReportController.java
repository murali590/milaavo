package com.milaavo.report.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import com.milaavo.report.dto.CreateReportRequest;
import com.milaavo.report.dto.CreateReportResponse;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import com.milaavo.report.service.ReportService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import com.milaavo.report.dto.PagedResponse;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    public ResponseEntity<CreateReportResponse> createReport(@Valid @RequestBody CreateReportRequest request) {
        
        CreateReportResponse response = reportService.createReport(request);
        return ResponseEntity.status( HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public ResponseEntity<PagedResponse<CreateReportResponse>> getAllReports(
        @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number must be at least 0") int page,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1") @Max(value = 100, message = "Size must be at most 100") int size
    ) {
        return ResponseEntity.ok(reportService.getAllReports(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreateReportResponse> getReportById(@PathVariable UUID id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }
    

    @PutMapping("/{id}")
    public ResponseEntity<CreateReportResponse> updateReport(@PathVariable UUID id, @Valid @RequestBody CreateReportRequest request) {  
        CreateReportResponse response = reportService.updateReport(id, request);
        return ResponseEntity.ok(response);
    }
    

    @DeleteMapping("/{id}") 
    public ResponseEntity<Void> deleteReport(@PathVariable UUID id) {
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}