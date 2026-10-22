package com.milaavo.service;

import com.milaavo.dto.CreateReportRequest;
import com.milaavo.dto.CreateReportResponse;
import com.milaavo.dto.PagedResponse;
import java.util.UUID;


public interface ReportService {

    CreateReportResponse createReport(CreateReportRequest request);

    PagedResponse<CreateReportResponse> getAllReports(int page, int size);

    CreateReportResponse getReportById(UUID id);

    CreateReportResponse updateReport(UUID id, CreateReportRequest request);

    void deleteReport(UUID id);

}