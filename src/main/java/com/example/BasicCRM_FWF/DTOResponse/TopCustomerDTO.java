package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopCustomerDTO {
    private String phoneNumber;
    private String customerName;
    private long bookingCount;
}
