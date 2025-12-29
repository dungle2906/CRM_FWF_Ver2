package com.example.basiccrmfwf.sales.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for SaleServiceItem.
 * This is the persistence representation - separate from domain model.
 */
@Entity
@Table(name = "sale_service_item")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaleServiceItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_transaction_id")
    private SalesTransactionEntity salesTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_type_id")
    private ServiceTypeEntity serviceType;
}
