package com.milaavo.report.service.impl;

import com.milaavo.report.service.ReportService;
import com.milaavo.report.dto.CreateReportRequest;
import com.milaavo.report.dto.CreateReportResponse;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.milaavo.report.repository.ReportRepository;
import com.milaavo.report.entity.LostReport;
import com.milaavo.exception.ReportNotFoundException;

import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import com.milaavo.report.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Optional;
import java.util.UUID;

@Service
class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Override
    @Transactional
    public CreateReportResponse createReport(CreateReportRequest request) {

        LostReport lostReport = new LostReport();
        lostReport.setItemName(request.getItemName());
        lostReport.setDescription(request.getDescription());
        lostReport.setCity(request.getCity());
        lostReport.setStatus("REPORTED");
        LostReport savedReport = reportRepository.save(lostReport);

        return new CreateReportResponse(
                savedReport.getId(),
                savedReport.getItemName(),
                savedReport.getDescription(),
                savedReport.getCity(),
                savedReport.getStatus());
    }

    @Override
    public PagedResponse<CreateReportResponse> getAllReports(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LostReport> lostReports = reportRepository.findAll(pageable);
        return new PagedResponse<>(
                lostReports.getContent().stream().map(report -> new CreateReportResponse(
                        report.getId(),
                        report.getItemName(),
                        report.getDescription(),
                        report.getCity(),
                        report.getStatus())).collect(Collectors.toList()),
                lostReports.getNumber(),
                lostReports.getSize(),
                lostReports.getTotalElements(),
                lostReports.getTotalPages(),
                lostReports.isFirst(),
                lostReports.isLast());
    }

    @Override
    public CreateReportResponse getReportById(UUID id) {

        LostReport report = reportRepository.findById(id)
                               .orElseThrow(() -> new ReportNotFoundException(id));
                               
        return new CreateReportResponse(
                report.getId(),
                report.getItemName(),
                report.getDescription(),
                report.getCity(),
                report.getStatus());
    }

    @Override
    @Transactional
    public CreateReportResponse updateReport(UUID id, CreateReportRequest request) {

        LostReport lostReport = reportRepository.findById(id)
                               .orElseThrow(() -> new ReportNotFoundException(id));
                               
        lostReport.setItemName(request.getItemName());
        lostReport.setDescription(request.getDescription());
        lostReport.setCity(request.getCity());
        LostReport updatedReport = reportRepository.save(lostReport);

        return new CreateReportResponse(
                updatedReport.getId(),
                updatedReport.getItemName(),
                updatedReport.getDescription(),
                updatedReport.getCity(),
                updatedReport.getStatus());
    }


    @Override
    @Transactional
    public void deleteReport(UUID id){

        LostReport lostReport = reportRepository.findById(id)
                               .orElseThrow(() -> new ReportNotFoundException(id));
        reportRepository.delete(lostReport);
    }
}