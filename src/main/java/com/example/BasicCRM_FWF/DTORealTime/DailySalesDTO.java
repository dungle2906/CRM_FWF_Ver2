package com.example.BasicCRM_FWF.DTORealTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailySalesDTO {
    private String date;
    private String totalRevenue;
    private String cash;
    private String transfer;
    private String card;
    private String foxieUsageRevenue;
    private String walletUsageRevenue;
    // Getters and setters
}