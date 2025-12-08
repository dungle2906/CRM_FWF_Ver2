package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSummaryDTO {
    // Khách mới
    private long totalNewCustomers;
    private long actualNewCustomers;
    private long previousNewCustomers;
    private long previousActualNewCustomers;
    private double growthTotalNew;
    private double growthActualNew;

    // Khách cũ
    private long totalOldCustomers;
    private long actualOldCustomers;
    private long previousOldCustomers;
    private long previousActualOldCustomers;
    private double growthTotalOld;
    private double growthActualOld;
}