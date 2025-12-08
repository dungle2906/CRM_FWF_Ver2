package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenderRevenueDTO {
    private BigDecimal avgActualRevenueMale;
    private BigDecimal avgActualRevenueFemale;
    private BigDecimal avgFoxieRevenueMale;
    private BigDecimal avgFoxieRevenueFemale;
}
