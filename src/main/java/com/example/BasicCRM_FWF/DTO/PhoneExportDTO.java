package com.example.BasicCRM_FWF.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PhoneExportDTO {
    private String phone;
    private CustomerSource source;
}
