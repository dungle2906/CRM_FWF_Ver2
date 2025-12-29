package com.example.basiccrmfwf.sales.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for SalesTransaction.
 * This is the persistence representation - separate from domain model.
 */
@Entity
@Table(name = "sales_transaction")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SalesTransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code")
    private Integer orderCode;

    @ManyToOne
    @JoinColumn(name = "facility_id")
    private RegionEntity facility;

    @Column(name = "order_date")
    private LocalDateTime orderDate;
    
    @Column(name = "customer_name")
    private String customerName;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "original_price")
    private BigDecimal originalPrice;
    
    @Column(name = "price_change")
    private BigDecimal priceChange;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @Column(name = "cash_transfer_credit")
    private BigDecimal cashTransferCredit;
    
    private BigDecimal cash;
    private BigDecimal transfer;
    
    @Column(name = "credit_card")
    private BigDecimal creditCard;
    
    private BigDecimal wallet;
    
    @Column(name = "prepaid_card")
    private BigDecimal prepaidCard;
    
    private BigDecimal debt;
    private String note;

    @Lob
    private String details;

    @ManyToOne
    @JoinColumn(name = "service_type_id")
    private ServiceTypeEntity serviceType;

    @OneToMany(mappedBy = "salesTransaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SaleServiceItemEntity> saleServiceItems;
    
    public List<SaleServiceItemEntity> getSaleServiceItems() {
        if (saleServiceItems == null) {
            saleServiceItems = new ArrayList<>();
        }
        return saleServiceItems;
    }
}
