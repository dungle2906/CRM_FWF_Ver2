package com.example.BasicCRM_FWF.DTORealTime;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BookingDTO {
    private String notConfirmed;
    private String confirmed;
    private String denied;
    private String customerCome;
    private String customerNotCome;
    private String cancel;
    private String autoConfirmed;


}
