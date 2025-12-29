package com.example.basiccrmfwf.sales.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for customer report queries.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerReportRequest {
    private LocalDateTime fromDate; // ngày bắt đầu user chọn
    private LocalDateTime toDate;   // ngày kết thúc user chọn
}
