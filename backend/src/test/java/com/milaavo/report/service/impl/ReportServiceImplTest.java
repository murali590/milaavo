package com.milaavo.report.service.impl;

import com.milaavo.report.dto.CreateReportRequest;
import com.milaavo.report.dto.CreateReportResponse;
import com.milaavo.report.dto.PagedResponse;
import com.milaavo.report.entity.ItemReport;
import com.milaavo.exception.ReportNotFoundException;
import com.milaavo.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private UUID reportId;
    private ItemReport existingReport;

    @BeforeEach
    void setUp() {
        reportId = UUID.randomUUID();

        existingReport = new ItemReport();
        existingReport.setId(reportId);
        existingReport.setItemName("Black wallet");
        existingReport.setDescription("Leather wallet");
        existingReport.setCity("Bengaluru");
        existingReport.setStatus("REPORTED");
    }

    @Test
    void createReport_shouldSaveAndReturnReport() {
        CreateReportRequest request = new CreateReportRequest();
        request.setItemName("Black wallet");
        request.setDescription("Leather wallet");
        request.setCity("Bengaluru");

        when(reportRepository.save(any(ItemReport.class)))
                .thenAnswer(invocation -> {
                    ItemReport report = invocation.getArgument(0);
                    report.setId(reportId);
                    return report;
                });

        CreateReportResponse response = reportService.createReport(request);

        assertEquals(reportId, response.reportId());
        assertEquals("Black wallet", response.itemName());
        assertEquals("Leather wallet", response.description());
        assertEquals("Bengaluru", response.city());
        assertEquals("REPORTED", response.status());

        verify(reportRepository).save(any(ItemReport.class));
    }

    @Test
    void updateReport_shouldUpdateExistingReport() {
        CreateReportRequest request = new CreateReportRequest();
        request.setItemName("Blue wallet");
        request.setDescription("Wallet with bank cards");
        request.setCity("Mysuru");

        when(reportRepository.findById(reportId))
                .thenReturn(Optional.of(existingReport));
        when(reportRepository.save(any(ItemReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateReportResponse response =
                reportService.updateReport(reportId, request);

        assertEquals(reportId, response.reportId());
        assertEquals("Blue wallet", response.itemName());
        assertEquals("Wallet with bank cards", response.description());
        assertEquals("Mysuru", response.city());
        assertEquals("REPORTED", response.status());

        verify(reportRepository).findById(reportId);
        verify(reportRepository).save(existingReport);
    }

    @Test
    void updateReport_shouldThrowWhenReportDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        CreateReportRequest request = new CreateReportRequest();
        request.setItemName("Blue wallet");
        request.setDescription("Wallet with bank cards");
        request.setCity("Mysuru");

        when(reportRepository.findById(missingId))
                .thenReturn(Optional.empty());

        assertThrows(
                ReportNotFoundException.class,
                () -> reportService.updateReport(missingId, request)
        );

        verify(reportRepository, never()).save(any(ItemReport.class));
    }

    @Test
    void deleteReport_shouldDeleteExistingReport() {
        when(reportRepository.findById(reportId))
                .thenReturn(Optional.of(existingReport));

        reportService.deleteReport(reportId);

        verify(reportRepository).findById(reportId);
        verify(reportRepository).delete(existingReport);
    }

    @Test
    void deleteReport_shouldThrowWhenReportDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        when(reportRepository.findById(missingId))
                .thenReturn(Optional.empty());

        assertThrows(
                ReportNotFoundException.class,
                () -> reportService.deleteReport(missingId)
        );

        verify(reportRepository, never()).delete(any(ItemReport.class));
    }

    @Test
void getReportById_shouldReturnReportWhenItExists() {
    when(reportRepository.findById(reportId))
            .thenReturn(Optional.of(existingReport));

    CreateReportResponse response = reportService.getReportById(reportId);

    assertEquals(reportId, response.reportId());
    assertEquals("Black wallet", response.itemName());
    assertEquals("Leather wallet", response.description());
    assertEquals("Bengaluru", response.city());
    assertEquals("REPORTED", response.status());

    verify(reportRepository).findById(reportId);
}

@Test
void getReportById_shouldThrowWhenReportDoesNotExist() {
    UUID missingId = UUID.randomUUID();

    when(reportRepository.findById(missingId))
            .thenReturn(Optional.empty());

    assertThrows(
            ReportNotFoundException.class,
            () -> reportService.getReportById(missingId)
    );

    verify(reportRepository).findById(missingId);
}

@Test
void getAllReports_shouldReturnReportsWithPaginationMetadata() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<ItemReport> reportPage =
            new PageImpl<>(List.of(existingReport), pageable, 1);

    when(reportRepository.findAll(any(Pageable.class)))
            .thenReturn(reportPage);

    PagedResponse<CreateReportResponse> response =
            reportService.getAllReports(0, 10);

    assertEquals(1, response.content().size());
    assertEquals(reportId, response.content().get(0).reportId());
    assertEquals(0, response.page());
    assertEquals(10, response.size());
    assertEquals(1, response.totalElements());
    assertEquals(1, response.totalPages());
    assertTrue(response.first());
    assertTrue(response.last());

    verify(reportRepository).findAll(any(Pageable.class));
}

@Test
void getAllReports_shouldReturnEmptyPageWhenNoReportsExist() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<ItemReport> emptyPage =
            new PageImpl<>(List.of(), pageable, 0);

    when(reportRepository.findAll(any(Pageable.class)))
            .thenReturn(emptyPage);

    PagedResponse<CreateReportResponse> response =
            reportService.getAllReports(0, 10);

    assertTrue(response.content().isEmpty());
    assertEquals(0, response.totalElements());
    assertEquals(0, response.totalPages());
    assertTrue(response.first());
    assertTrue(response.last());
}

}