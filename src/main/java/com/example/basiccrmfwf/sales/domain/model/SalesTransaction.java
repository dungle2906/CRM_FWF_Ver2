package com.example.basiccrmfwf.sales.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a sales transaction.
 * Pure domain model - no JPA annotations, no Spring dependencies.
 * Contains business logic methods.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesTransaction {
    private Long id;
    private Integer orderCode;
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
    private String details;
    private SaleServiceItem.ServiceType serviceType;
    private List<SaleServiceItem> saleServiceItems;
    
    /**
     * Business logic: Calculate actual revenue (cash + transfer + credit).
     */
    public BigDecimal calculateActualRevenue() {
        if (cashTransferCredit != null) {
            return cashTransferCredit;
        }
        BigDecimal actual = BigDecimal.ZERO;
        if (cash != null) actual = actual.add(cash);
        if (transfer != null) actual = actual.add(transfer);
        if (creditCard != null) actual = actual.add(creditCard);
        return actual;
    }
    
    /**
     * Business logic: Check if transaction has prepaid card payment.
     */
    public boolean hasPrepaidCardPayment() {
        return prepaidCard != null && prepaidCard.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Business logic: Get total payment amount.
     */
    public BigDecimal getTotalPayment() {
        BigDecimal total = BigDecimal.ZERO;
        if (cash != null) total = total.add(cash);
        if (transfer != null) total = total.add(transfer);
        if (creditCard != null) total = total.add(creditCard);
        if (wallet != null) total = total.add(wallet);
        if (prepaidCard != null) total = total.add(prepaidCard);
        if (debt != null) total = total.add(debt);
        return total;
    }
    
    /**
     * Initialize saleServiceItems list if null.
     */
    public List<SaleServiceItem> getSaleServiceItems() {
        if (saleServiceItems == null) {
            saleServiceItems = new ArrayList<>();
        }
        return saleServiceItems;
    }
}
