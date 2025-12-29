package com.example.basiccrmfwf.sales.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class RegionRevenuePieDTO {
    private String region;
    private BigDecimal actualRevenue;
    private double revenuePercent;

    public RegionRevenuePieDTO(String region, BigDecimal actualRevenue, double revenuePercent) {
        this.region = region;
        this.actualRevenue = actualRevenue;
        this.revenuePercent = revenuePercent;
    }
}
