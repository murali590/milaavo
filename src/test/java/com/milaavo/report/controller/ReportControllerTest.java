
package com.milaavo.report.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milaavo.report.dto.CreateReportRequest;
import com.milaavo.report.dto.CreateReportResponse;
import com.milaavo.exception.GlobalExceptionHandler;
import com.milaavo.exception.ReportNotFoundException;
import com.milaavo.report.service.ReportService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(GlobalExceptionHandler.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

   private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ReportService reportService;

    private final UUID reportId =
            UUID.fromString("2679d0f4-f0b4-4388-8fec-239d42637004");

    @Test
    void createReport_shouldReturn201() throws Exception {
        CreateReportRequest request = createRequest();
        CreateReportResponse response = createResponse();

        when(reportService.createReport(any(CreateReportRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(reportId.toString()))
                .andExpect(jsonPath("$.itemName").value("Black wallet"))
                .andExpect(jsonPath("$.status").value("REPORTED"));
    }

    @Test
    void createReport_shouldReturn400ForInvalidRequest() throws Exception {
        String invalidRequest = """
                {
                    "itemName": " ",
                    "description": "Leather wallet",
                    "city": "Bengaluru"
                }
                """;

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.itemName")
                        .value("Item name is required"));
    }

    @Test
    void getReportById_shouldReturn200() throws Exception {
        when(reportService.getReportById(reportId))
                .thenReturn(createResponse());

        mockMvc.perform(get("/api/reports/{id}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(reportId.toString()))
                .andExpect(jsonPath("$.itemName").value("Black wallet"));
    }

    @Test
    void getReportById_shouldReturn404WhenMissing() throws Exception {
        when(reportService.getReportById(reportId))
                .thenThrow(new ReportNotFoundException(reportId));

        mockMvc.perform(get("/api/reports/{id}", reportId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("REPORT_NOT_FOUND"));
    }

    @Test
    void updateReport_shouldReturn200() throws Exception {
        CreateReportRequest request = createRequest();
        CreateReportResponse response = new CreateReportResponse(
                reportId,
                "Blue wallet",
                "Wallet with bank cards",
                "Mysuru",
                "REPORTED"
        );

        when(reportService.updateReport(eq(reportId), any(CreateReportRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/reports/{id}", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value("Blue wallet"))
                .andExpect(jsonPath("$.city").value("Mysuru"));
    }

    @Test
    void deleteReport_shouldReturn204() throws Exception {
        doNothing().when(reportService).deleteReport(reportId);

        mockMvc.perform(delete("/api/reports/{id}", reportId))
                .andExpect(status().isNoContent());

        verify(reportService).deleteReport(reportId);
    }

    @Test
void getAllReports_shouldReturn400ForNegativePage() throws Exception {
    mockMvc.perform(get("/api/reports")
                    .param("page", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
}

@Test
void getAllReports_shouldReturn400WhenSizeExceeds100() throws Exception {
    mockMvc.perform(get("/api/reports")
                    .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
}

    @Test
    void deleteReport_shouldReturn404WhenReportDoesNotExist() throws Exception {
        doThrow(new ReportNotFoundException(reportId))
                .when(reportService).deleteReport(reportId);

        mockMvc.perform(delete("/api/reports/{id}", reportId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("REPORT_NOT_FOUND"));
    }

    private CreateReportRequest createRequest() {
        CreateReportRequest request = new CreateReportRequest();
        request.setItemName("Black wallet");
        request.setDescription("Leather wallet");
        request.setCity("Bengaluru");
        return request;
    }

    private CreateReportResponse createResponse() {
        return new CreateReportResponse(
                reportId,
                "Black wallet",
                "Leather wallet",
                "Bengaluru",
                "REPORTED"
        );
    }
}
