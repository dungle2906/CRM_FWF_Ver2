package com.example.BasicCRM_FWF.Controller;

import com.example.BasicCRM_FWF.DTOResponse.ShiftDTO;
import com.example.BasicCRM_FWF.Service.Realtime.RealTimeInterface;
import com.example.BasicCRM_FWF.Service.ShiftEmployee.ShiftEmployeeInterface;
import com.example.BasicCRM_FWF.Service.ShiftEmployee.ShiftEmployeeService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/shift")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftEmployeeInterface service;
    private final RealTimeInterface realTimeService;

//    @PostMapping("/upload")
//    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
//        service.importFromExcel(file);
//        return ResponseEntity.ok("Upload successful" + file.getOriginalFilename());
//    }

//    @GetMapping("/shift-employee")
//    public ResponseEntity<List<ShiftDTO>> workTract() throws Exception {
//        List<ShiftDTO> shiftDTOS = realTimeService.autoSaveWorkTrack();
//        return ResponseEntity.ok(shiftDTOS);
//    }

//    @GetMapping("/get-all-shift")
//    public ResponseEntity<List<ShiftDTO>> getAllShift(
//    ){
//        // Use .ok() because the service returns a List directly, not an Optional.
//        // .of() expects an Optional.
//        List<ShiftDTO> shifts = service.shiftEmployee();
//
//        return ResponseEntity.ok(shifts); // Returns 200 OK with the list as the body
//    }

    @GetMapping("/get-shift-by-date")
    public ResponseEntity<List<ShiftDTO>> getShiftByDate(
            @RequestParam LocalDate date
    ) {
        List<ShiftDTO> shifts = service.shiftEmployeeByDate(date);
        return ResponseEntity.ok(shifts);
    }
}
