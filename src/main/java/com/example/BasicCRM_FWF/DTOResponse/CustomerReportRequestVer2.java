package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerReportRequestVer2 {
    private LocalDateTime fromDate; // ngày bắt đầu user chọn
    private LocalDateTime toDate;   // ngày kết thúc user chọn
    private String status;
}