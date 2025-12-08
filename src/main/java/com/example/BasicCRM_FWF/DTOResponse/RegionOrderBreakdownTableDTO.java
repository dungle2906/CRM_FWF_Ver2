package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegionOrderBreakdownTableDTO {
    private String shopName;
    private Long totalOrders;

    private Long serviceOrders;              // DV%
    private Long prepaidCard;   // s.cash_transfer_credit > 0
    private Long comboOrders;                // CB%
    private Long cardPurchaseOrders;         // Foxie Member Card / Foxie Card%

    private Long deltaTotalOrders;
    private Long deltaServiceOrders;
    private Long deltaPrepaidCard;
    private Long deltaComboOrders;
    private Long deltaCardPurchaseOrders;
}
