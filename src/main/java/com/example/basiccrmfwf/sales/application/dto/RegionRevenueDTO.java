package com.example.basiccrmfwf.sales.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for region revenue data.
 */
@Data
public class RegionRevenueDTO {
    private String region;
    private LocalDate date;
    private BigDecimal totalRevenue;

    public RegionRevenueDTO(String region, LocalDate date, BigDecimal totalRevenue) {
        this.region = region;
        this.date = date;
        this.totalRevenue = totalRevenue;
    }
}
