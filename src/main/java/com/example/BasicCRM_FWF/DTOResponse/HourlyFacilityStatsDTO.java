package com.example.BasicCRM_FWF.DTOResponse;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class HourlyFacilityStatsDTO {
    private String facility;
    private Map<String, Integer> hourlyCounts;
    private Integer total;

    public HourlyFacilityStatsDTO(String facility) {
        this.facility = facility;
        this.hourlyCounts = new LinkedHashMap<>();
        this.total = 0;
    }

    public void addCount(int hour, int i) {
        String range = hour + "-" + (hour + 1);
        this.hourlyCounts.put(range, this.hourlyCounts.getOrDefault(range, 0) + 1);
        this.total++;
    }
}
