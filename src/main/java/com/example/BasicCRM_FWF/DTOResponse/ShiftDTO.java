package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShiftDTO {
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
