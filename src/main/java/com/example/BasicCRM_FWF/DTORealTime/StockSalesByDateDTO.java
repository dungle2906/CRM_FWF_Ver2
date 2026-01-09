package com.example.BasicCRM_FWF.DTORealTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
public class StockSalesByDateDTO {
    private String stockId;
    private List<DailySalesDTO> days;

    public StockSalesByDateDTO(String stockId, List<DailySalesDTO> days) {
        this.stockId = stockId;
        this.days = days;
    }
    // Getters and setters
}