package com.example.BasicCRM_FWF.Model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;

//@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@SuperBuilder
@Builder
@Table(name = "shift")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Integer id;

    private String fullname;
    private String username;
    private String stockId;
    private String stockTitle;
    private String date;
    private String checkIn;
    private String checkOut;
    private String title;
    private String timeFrom;
    private String timeTo;
    private String mandays;
    private String typeCheckIn;
    private String desTypeCheckIn;
    private String typeCheckOut;
    private String desTypeCheckOut;

    private Double diSom;   // đi sớm
    private Double diMuon;  // đi muộn
    private Double veSom;   // về sớm
    private Double veMuon;  // về muộn
}
