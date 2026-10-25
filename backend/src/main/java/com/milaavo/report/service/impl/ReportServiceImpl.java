package com.milaavo.report.service.impl;

import com.milaavo.common.security.AuthUser;
import com.milaavo.exception.ReportNotFoundException;
import com.milaavo.report.dto.CreateReportRequest;
import com.milaavo.report.dto.CreateReportResponse;
import com.milaavo.report.dto.PagedResponse;
import com.milaavo.report.entity.LostReport;
import com.milaavo.report.repository.ReportRepository;
import com.milaavo.report.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Override
    @Transactional
    public CreateReportResponse createReport(AuthUser currentUser, CreateReportRequest request) {

        LostReport lostReport = new LostReport();
        lostReport.setOwnerId(currentUser.userId());
        lostReport.setItemName(request.getItemName());
        lostReport.setDescription(request.getDescription());
        lostReport.setCity(request.getCity());
        lostReport.setStatus("REPORTED");

        return toResponse(reportRepository.save(lostReport));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CreateReportResponse> getAllReports(AuthUser currentUser, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LostReport> lostReports =
                reportRepository.findByOwnerId(currentUser.userId(), pageable);

        return new PagedResponse<>(
                lostReports.getContent().stream().map(this::toResponse).toList(),
                lostReports.getNumber(),
                lostReports.getSize(),
                lostReports.getTotalElements(),
                lostReports.getTotalPages(),
                lostReports.isFirst(),
                lostReports.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public CreateReportResponse getReportById(AuthUser currentUser, UUID id) {
        return toResponse(findOwnedReport(currentUser, id));
    }

    @Override
    @Transactional
    public CreateReportResponse updateReport(AuthUser currentUser, UUID id, CreateReportRequest request) {

        LostReport lostReport = findOwnedReport(currentUser, id);
        lostReport.setItemName(request.getItemName());
        lostReport.setDescription(request.getDescription());
        lostReport.setCity(request.getCity());

        return toResponse(reportRepository.save(lostReport));
    }

    @Override
    @Transactional
    public void deleteReport(AuthUser currentUser, UUID id) {
        reportRepository.delete(findOwnedReport(currentUser, id));
    }

    // Not found and "belongs to someone else" both give 404, so ids can't be probed.
    private LostReport findOwnedReport(AuthUser currentUser, UUID id) {
        return reportRepository.findByIdAndOwnerId(id, currentUser.userId())
                .orElseThrow(() -> new ReportNotFoundException(id));
    }

    private CreateReportResponse toResponse(LostReport report) {
        return new CreateReportResponse(
                report.getId(),
                report.getItemName(),
                report.getDescription(),
                report.getCity(),
                report.getStatus());
    }
}