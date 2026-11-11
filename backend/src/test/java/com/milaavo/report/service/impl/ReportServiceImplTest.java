package com.milaavo.report.service.impl;

import com.milaavo.common.security.AuthUser;
import com.milaavo.exception.ReportNotFoundException;
import com.milaavo.report.dto.CreateReportRequest;
import com.milaavo.report.dto.CreateReportResponse;
import com.milaavo.report.dto.PagedResponse;
import com.milaavo.report.entity.ItemReport;
import com.milaavo.report.enums.ReportType;
import com.milaavo.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private final AuthUser me = new AuthUser(UUID.randomUUID());
    private final UUID reportId = UUID.randomUUID();
    private ItemReport myReport;

    @BeforeEach
    void setUp() {
        myReport = new ItemReport();
        myReport.setId(reportId);
        myReport.setOwnerId(me.userId());
        myReport.setType(ReportType.LOST);
        myReport.setItemName("Black wallet");
        myReport.setDescription("Leather wallet");
        myReport.setCity("Bengaluru");
        myReport.setStatus("REPORTED");
    }

    @Test
    void createReport_setsOwnerFromLoggedInUser() {
        when(reportRepository.saveAndFlush(any(ItemReport.class)))
                .thenAnswer(call -> call.getArgument(0));

        CreateReportResponse response = reportService.createReport(me, request(ReportType.FOUND, "Blue bottle"));

        ArgumentCaptor<ItemReport> saved = ArgumentCaptor.forClass(ItemReport.class);
        verify(reportRepository).saveAndFlush(saved.capture());
        assertEquals(me.userId(), saved.getValue().getOwnerId());
        assertEquals("REPORTED", saved.getValue().getStatus());
        assertEquals(ReportType.FOUND, response.type());
        assertEquals("Blue bottle", response.itemName());
    }

    @Test
    void getAllReports_withoutType_listsOnlyMyReportsNewestFirst() {
        when(reportRepository.findByOwnerId(eq(me.userId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(myReport)));

        PagedResponse<CreateReportResponse> page = reportService.getAllReports(me, null, 0, 10);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reportRepository).findByOwnerId(eq(me.userId()), pageable.capture());
        assertEquals(Sort.by("createdAt").descending(), pageable.getValue().getSort());
        assertEquals(1, page.content().size());
        verify(reportRepository, never()).findByOwnerIdAndType(any(), any(), any());
    }

    @Test
    void getAllReports_withType_usesTypeFilter() {
        when(reportRepository.findByOwnerIdAndType(eq(me.userId()), eq(ReportType.LOST), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(myReport)));

        PagedResponse<CreateReportResponse> page = reportService.getAllReports(me, ReportType.LOST, 0, 10);

        assertEquals(ReportType.LOST, page.content().get(0).type());
        verify(reportRepository, never()).findByOwnerId(any(), any());
    }

    @Test
    void getReportById_ofSomeoneElse_isNotFound() {
        // The repository only finds by (id, ownerId), so another user's report looks missing.
        when(reportRepository.findByIdAndOwnerId(reportId, me.userId())).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class, () -> reportService.getReportById(me, reportId));
    }

    @Test
    void updateReport_ofSomeoneElse_isNotFoundAndNothingSaved() {
        when(reportRepository.findByIdAndOwnerId(reportId, me.userId())).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class,
                () -> reportService.updateReport(me, reportId, request(ReportType.LOST, "Changed")));

        verify(reportRepository, never()).save(any());
    }

    @Test
    void updateReport_ofMine_changesFields() {
        when(reportRepository.findByIdAndOwnerId(reportId, me.userId())).thenReturn(Optional.of(myReport));
        when(reportRepository.save(myReport)).thenReturn(myReport);

        CreateReportResponse response = reportService.updateReport(me, reportId,
                request(ReportType.SWAPPED, "Grey laptop bag"));

        assertEquals("Grey laptop bag", response.itemName());
        assertEquals(ReportType.SWAPPED, response.type());
        assertEquals(me.userId(), myReport.getOwnerId()); // owner never changes
    }

    @Test
    void deleteReport_ofSomeoneElse_isNotFoundAndNothingDeleted() {
        when(reportRepository.findByIdAndOwnerId(reportId, me.userId())).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class, () -> reportService.deleteReport(me, reportId));

        verify(reportRepository, never()).delete(any());
    }

    @Test
    void deleteReport_ofMine_deletesIt() {
        when(reportRepository.findByIdAndOwnerId(reportId, me.userId())).thenReturn(Optional.of(myReport));

        reportService.deleteReport(me, reportId);

        verify(reportRepository).delete(myReport);
    }

    private static CreateReportRequest request(ReportType type, String itemName) {
        return CreateReportRequest.builder()
                .type(type)
                .itemName(itemName)
                .description("Some description")
                .city("Bengaluru")
                .build();
    }
}