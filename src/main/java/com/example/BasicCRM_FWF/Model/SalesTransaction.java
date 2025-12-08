package com.example.BasicCRM_FWF.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_transaction")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SalesTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer orderCode;

    @ManyToOne
    @JoinColumn(name = "facility_id")
    private Region facility;

    private LocalDateTime orderDate;
    private String customerName;
    private String phoneNumber;
    private BigDecimal originalPrice;
    private BigDecimal priceChange;
    private BigDecimal totalAmount;
    private BigDecimal cashTransferCredit;
    private BigDecimal cash;
    private BigDecimal transfer;
    private BigDecimal creditCard;
    private BigDecimal wallet;
    private BigDecimal prepaidCard;
    private BigDecimal debt;
    private String note;

    @Lob
    private String details;

    @ManyToOne
    @JoinColumn(name = "service_type_id")
    private ServiceType serviceType;

    @OneToMany(mappedBy = "salesTransaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SaleServiceItem> saleServiceItems;
}