package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TotalCustomerResponse {
    // Tổng số giao dịch
    private long currentTotal;
    private long previousTotal;
    private double changePercentTotal;

    // Theo giới tính (đếm theo record)
    private long currentMale;
    private long previousMale;
    private double changePercentMale;

    private long currentFemale;
    private long previousFemale;
    private double changePercentFemale;
}
