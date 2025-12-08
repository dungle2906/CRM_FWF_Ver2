package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class PaymentBreakdownDTO {
    private BigDecimal totalCash;
    private BigDecimal totalTransfer;
    private BigDecimal totalCreditCard;
    private BigDecimal totalPrepaidCard;
    private BigDecimal totalDebt;

}