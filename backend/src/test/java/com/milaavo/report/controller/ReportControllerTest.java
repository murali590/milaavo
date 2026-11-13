package com.milaavo.report.controller;

import com.milaavo.common.security.AuthUser;
import com.milaavo.common.security.AuthUserJwtConverter;
import com.milaavo.config.ApiSecurityErrorHandler;
import com.milaavo.config.CorsProperties;
import com.milaavo.config.SecurityConfig;
import com.milaavo.exception.ReportNotFoundException;
import com.milaavo.report.dto.CreateReportRequest;
import com.milaavo.report.dto.CreateReportResponse;
import com.milaavo.report.dto.PagedResponse;
import com.milaavo.report.enums.ReportType;
import com.milaavo.report.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class, properties = "spring.profiles.active=test")
@Import({ SecurityConfig.class, ApiSecurityErrorHandler.class, AuthUserJwtConverter.class })
@EnableConfigurationProperties(CorsProperties.class)
class ReportControllerTest {

    private static final String TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    // Fake the signature check only. Our real SecurityConfig and
    // AuthUserJwtConverter still run.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final AuthUser me = new AuthUser(UUID.randomUUID());
    private final UUID reportId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "HS256")
                .subject(me.userId().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        when(jwtDecoder.decode(TOKEN)).thenReturn(jwt);
    }

    @Test
    void noToken_gives401InOurErrorShape() throws Exception {
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("BROKEN_ON_PURPOSE"));

        verifyNoInteractions(reportService);
    }

    @Test
    void createReport_passesLoggedInUserToService_andReturns201() throws Exception {
        when(reportService.createReport(eq(me), any(CreateReportRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "type": "LOST", "itemName": "Black wallet",
                          "description": "Leather wallet", "city": "Bengaluru" }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(reportId.toString()))
                .andExpect(jsonPath("$.type").value("LOST"));

        verify(reportService).createReport(eq(me), any(CreateReportRequest.class));
    }

    @Test
    void createReport_withBlankNameAndNoType_gives400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "itemName": " ", "description": "Leather wallet", "city": "Bengaluru" }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.itemName").value("Item name is required"))
                .andExpect(jsonPath("$.fieldErrors.type").value("Report type is required"));

        verifyNoInteractions(reportService);
    }

    @Test
    void getAllReports_passesTypeFilter() throws Exception {
        when(reportService.getAllReports(me, ReportType.FOUND, 0, 10))
                .thenReturn(new PagedResponse<>(List.of(response()), 0, 10, 1, 1, true, true));

        mockMvc.perform(get("/api/reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .param("type", "FOUND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllReports_withSizeOver100_gives400() throws Exception {
        mockMvc.perform(get("/api/reports")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void getReportById_withNonUuid_gives400() throws Exception {
        mockMvc.perform(get("/api/reports/not-a-uuid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    void deleteReport_notMineOrMissing_gives404() throws Exception {
        doThrow(new ReportNotFoundException(reportId)).when(reportService).deleteReport(me, reportId);

        mockMvc.perform(delete("/api/reports/{id}", reportId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("REPORT_NOT_FOUND"));
    }

    @Test
    void deleteReport_mine_gives204() throws Exception {
        mockMvc.perform(delete("/api/reports/{id}", reportId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isNoContent());

        verify(reportService).deleteReport(me, reportId);
    }

    private CreateReportResponse response() {
        return new CreateReportResponse(reportId, ReportType.LOST, "Black wallet", "Leather wallet",
                "Bengaluru", "REPORTED", Instant.parse("2026-11-11T05:00:00Z"));
    }
}