package com.example.BasicCRM_FWF.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "service_type")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String service_code;

    private String service_name;

    private BigDecimal price;

    private String category;

    @OneToMany(mappedBy = "serviceType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SaleServiceItem> saleServiceItems;
}
