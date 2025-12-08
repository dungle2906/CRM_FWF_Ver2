package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreRevenueStatDTO {
    private String storeName;
    private long currentOrders;
    private long deltaOrders;
    private BigDecimal cashTransfer;
    private BigDecimal prepaidCard;
    private double revenueGrowth;
    private double cashPercent;
    private double prepaidPercent;
    private double orderPercent;
}

