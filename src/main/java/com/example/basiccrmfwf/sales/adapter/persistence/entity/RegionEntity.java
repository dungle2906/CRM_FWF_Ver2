package com.example.basiccrmfwf.sales.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for Region (facility/shop).
 * This is the persistence representation - separate from domain model.
 */
@Entity
@Table(name = "region")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "shop_type")
    private String shopType;

    private String region;

    @Column(name = "stock_id")
    private String stockId;
}
