package com.example.BasicCRM_FWF.Service.ShiftEmployee;

import com.example.BasicCRM_FWF.DTOResponse.ShiftDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Time;
import java.time.LocalDate;
import java.util.List;

public interface ShiftEmployeeInterface {
//    public void importFromExcel(MultipartFile file);

    List<ShiftDTO> shiftEmployee();

    List<ShiftDTO> shiftEmployeeByDate(LocalDate date);
}
