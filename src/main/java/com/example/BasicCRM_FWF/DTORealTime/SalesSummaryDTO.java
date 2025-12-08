package com.example.BasicCRM_FWF.DTORealTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesSummaryDTO {
    private String totalRevenue;
    private String cash;
    private String transfer;
    private String card;
    private String actualRevenue;
    private String foxieUsageRevenue;
    private String walletUsageRevenue;
    private String toPay;
    private String debt;
}