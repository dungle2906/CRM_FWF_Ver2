package com.example.BasicCRM_FWF.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@SuperBuilder
@Table(name = "booking_record")
public class BookingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Integer id;

    private LocalDateTime created_date;
    private LocalDateTime booking_date;

    @ManyToOne
    @JoinColumn(name = "facility_id")
    private Region facility;

    private String customer_name;
    private String phone_number;

    @Column(length = 500)
    private String service_name;

    @ManyToOne
    @JoinColumn(name = "booking_status_id")
    private BookingStatus bookingStatus;

    private String bookingEmployee;
    private boolean customerStatus;
    private Integer customer_amount;
}
