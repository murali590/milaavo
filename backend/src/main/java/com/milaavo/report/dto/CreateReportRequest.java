package com.milaavo.report.dto;

import com.milaavo.report.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class CreateReportRequest {

    @NotBlank(message = "Item name is required")
    @Size(max = 200, message = "Item name must be less than 200 characters")
    private String itemName;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City name must be less than 100 characters")
    private String city;

    @NotNull(message = "Report type is required")
    private ReportType type;

}