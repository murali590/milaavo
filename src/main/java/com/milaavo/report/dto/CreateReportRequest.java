package com.milaavo.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}