package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingStatusStatsDTO {
    private String status;
    private Long count;
}
