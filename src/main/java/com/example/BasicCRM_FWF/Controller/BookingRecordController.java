package com.example.BasicCRM_FWF.Controller;

import com.example.BasicCRM_FWF.DTORequest.CustomerReportRequest;
import com.example.BasicCRM_FWF.DTOResponse.*;
import com.example.BasicCRM_FWF.Service.BookingRecord.BookingRecordInterface;
import com.example.BasicCRM_FWF.Service.BookingRecord.BookingRecordService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/booking")
@AllArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CEO', 'TEAM_LEAD')")
public class BookingRecordController {

    private final BookingRecordInterface service;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        service.importFromExcel(file);
        return ResponseEntity.ok("Upload completed. Check logs for details." + file.getOriginalFilename());
    }

    // Table thời gian đặt lịch
    @PostMapping("/facility-booking-hour")
    public ResponseEntity<List<HourlyFacilityStatsDTO>> facilityBookingHour(@RequestBody CustomerReportRequestVer2 request) {
        List<HourlyFacilityStatsDTO> stats = service.getHourlyArrivalStats(request);
        return ResponseEntity.ok(stats);
    }

    // Trạng thái booking status count
    @PostMapping("/booking-status-stats")
    public ResponseEntity<List<BookingStatusStatsDTO>> getBookingStatusStats(@RequestBody CustomerReportRequest request) {
        List<BookingStatusStatsDTO> stats = service.getBookingStatusStats(request.getFromDate(), request.getToDate());
        return ResponseEntity.ok(stats);
    }

    // Tỉ lệ khách cũ khách mới sử dụng dịch vụ
    @PostMapping("/customer-status-ratio")
    public ResponseEntity<CustomerStatusRatioDTO> getCustomerStatusRatio(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getCustomerStatusRatio(
                request.getFromDate(), request.getToDate()));
    }

    // Top khách hàng sử dụng dịch vụ
    @PostMapping("/top-customers")
    public ResponseEntity<List<TopCustomerDTO>> getTopCustomers(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getTopCustomers(request.getFromDate(), request.getToDate()));
    }

    // Top nhân viên booking
    @PostMapping("/top-booking")
    public ResponseEntity<List<TopEmployeeDTO>> getTopBookingEmployee(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getTopBookingEmployee(request.getFromDate(), request.getToDate()));
    }
}
