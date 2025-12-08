package com.example.BasicCRM_FWF.DTO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class FullDateRangeResponse {
    private LocalDateTime globalMin;
    private LocalDateTime globalMax;

    private Map<String, LocalDateTime> minMap;
    private Map<String, LocalDateTime> maxMap;

    private String message; // để hiển thị popup
}
