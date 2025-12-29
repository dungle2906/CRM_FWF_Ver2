package com.example.basiccrmfwf.sales.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * JPA entity for ServiceType.
 * This is the persistence representation - separate from domain model.
 */
@Entity
@Table(name = "service_type")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceTypeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_code")
    private String serviceCode;

    @Column(name = "service_name")
    private String serviceName;

    private BigDecimal price;

    private String category;

    @OneToMany(mappedBy = "serviceType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SaleServiceItemEntity> saleServiceItems;
}
