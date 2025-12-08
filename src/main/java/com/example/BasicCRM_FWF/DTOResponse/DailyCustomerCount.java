package com.example.BasicCRM_FWF.DTOResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date; // hoặc java.time.LocalDate nếu bạn ép lại trong query

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DailyCustomerCount {
    private Date date;
    private Long count;
}
