package com.example.BasicCRM_FWF.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sale_service_item")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaleServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_transaction_id")
    private SalesTransaction salesTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_type_id")
    private ServiceType serviceType;
}