package com.milaavo.report.service;

import com.milaavo.report.dto.CreateReportRequest;
import com.milaavo.report.dto.CreateReportResponse;
import com.milaavo.report.dto.PagedResponse;
import java.util.UUID;


public interface ReportService {

    CreateReportResponse createReport(CreateReportRequest request);

    PagedResponse<CreateReportResponse> getAllReports(int page, int size);

    CreateReportResponse getReportById(UUID id);

    CreateReportResponse updateReport(UUID id, CreateReportRequest request);

    void deleteReport(UUID id);

}