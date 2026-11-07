package com.milaavo.report.service;

import com.milaavo.common.security.AuthUser;
import com.milaavo.report.dto.CreateReportRequest;
import com.milaavo.report.dto.CreateReportResponse;
import com.milaavo.report.dto.PagedResponse;
import com.milaavo.report.enums.ReportType;

import java.util.UUID;

public interface ReportService {

    CreateReportResponse createReport(AuthUser currentUser, CreateReportRequest request);

    PagedResponse<CreateReportResponse> getAllReports(AuthUser currentUser, ReportType type, int page, int size);

    CreateReportResponse getReportById(AuthUser currentUser, UUID id);

    CreateReportResponse updateReport(AuthUser currentUser, UUID id, CreateReportRequest request);

    void deleteReport(AuthUser currentUser, UUID id);
}