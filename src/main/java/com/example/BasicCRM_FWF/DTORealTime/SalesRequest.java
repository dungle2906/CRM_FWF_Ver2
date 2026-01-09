package com.example.BasicCRM_FWF.DTORealTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesRequest {
    private List<String> stockIds;
    private List<String> dates;
    // Getters and setters
}
