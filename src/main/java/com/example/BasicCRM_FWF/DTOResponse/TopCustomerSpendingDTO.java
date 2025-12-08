package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopCustomerSpendingDTO {
    private String phoneNumber;
    private String customerName;
    private BigDecimal totalSpending;
}
