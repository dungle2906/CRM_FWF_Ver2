package com.example.basiccrmfwf.sales.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing a facility/region (shop location).
 * Pure domain model - no JPA annotations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Region {
    private Long id;
    private String shopName;
    private String shopType;
    private String region;
    private String stockId;
}
