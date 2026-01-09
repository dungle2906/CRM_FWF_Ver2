package com.example.BasicCRM_FWF.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "booking_sales_item", uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id", "sales_transaction_id"}))
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingSalesMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    private BookingRecord booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_transaction_id")
    private SalesTransaction salesTransaction;
}