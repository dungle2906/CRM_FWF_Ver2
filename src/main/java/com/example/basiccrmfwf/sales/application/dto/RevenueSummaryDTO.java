package com.example.basiccrmfwf.sales.application.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Response DTO for revenue summary.
 */
@Data
public class RevenueSummaryDTO {
    private BigDecimal totalRevenue;
    private BigDecimal actualRevenue;
    private double revenueGrowth;
    private double actualGrowth;

    public RevenueSummaryDTO(BigDecimal totalRevenue, BigDecimal actualRevenue,
                              double revenueGrowth, double actualGrowth) {
        this.totalRevenue = totalRevenue;
        this.actualRevenue = actualRevenue;
        this.revenueGrowth = revenueGrowth;
        this.actualGrowth = actualGrowth;
    }
}
