package com.example.BasicCRM_FWF.Controller;

import com.example.BasicCRM_FWF.DTORealTime.*;
import com.example.BasicCRM_FWF.Service.JWTService;
import com.example.BasicCRM_FWF.Service.Realtime.RealTimeInterface;
import com.example.BasicCRM_FWF.Service.Realtime.RealTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/real-time")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CEO', 'TEAM_LEAD', 'AREA_MANAGER', 'STORE_LEAD', 'USER')")
public class RealTimeController {

    private final RealTimeInterface salesService;
    private final JWTService jwtService;

    // Bảng tổng doanh số
    @GetMapping("/sales-summary")
    public SalesSummaryDTO getSalesSummary(
            @RequestParam String dateStart,
            @RequestParam String dateEnd,
            @RequestParam String stockId
    ) throws Exception {
        return salesService.getSales(dateStart, dateEnd, stockId);
    }

    // Bảng tổng doanh số copiedz
    @GetMapping("/sales-summary-copied")
    public SalesSummaryDTO getSalesSummaryCopied(
            @RequestParam String dateStart,
            @RequestParam String dateEnd,
            @RequestParam String stockId
    ) throws Exception {
        return salesService.getSalesCopied(dateStart, dateEnd, stockId);
    }

    // Lấy thực thu, phục vụ cho target KPI
    @GetMapping("/get-actual-revenue")
    public ResponseEntity<String> getActualRevenue(
            @RequestParam String dateStart,
            @RequestParam String dateEnd,
            @RequestParam String stockId
    ) throws Exception {
        String actual = salesService.getActualRevenue(dateStart, dateEnd, stockId);
        return ResponseEntity.ok(actual);
    }

    // Dịch vụ đang sử dụng real-time
    @GetMapping("/service-summary")
    public ServiceSummaryDTO getServiceSummary(
            @RequestParam String dateStart,
            @RequestParam String dateEnd,
            @RequestParam String stockId
    ) throws Exception {
        return salesService.getServiceSummary(dateStart, dateEnd, stockId);
    }

    // Lấy top 10 dịch vụ sử dụng real time
    @GetMapping("/get-top-10-service")
    public List<ServiceItems> getTop10Service(
            @RequestParam String dateStart,
            @RequestParam String dateEnd,
            @RequestParam String stockId
    ) throws Exception {
        return salesService.getTop10Service(dateStart, dateEnd, stockId);
    }

    // Chi tiết doanh thu
    @GetMapping("/sales-detail")
    public List<SalesDetailDTO> getSalesDetail(
            @RequestParam String dateStart,
            @RequestParam String dateEnd
    ) throws Exception {
        return salesService.getSalesDetail(dateStart, dateEnd);
    }

    // Panel hiển thị khúc đặt lịch
    @GetMapping("/booking")
    public BookingDTO getBooking(
            @RequestParam String dateStart,
            @RequestParam String dateEnd,
            @RequestParam String stockId
    ) throws Exception {
        return salesService.getBookings(dateStart, dateEnd, stockId);
    }

    // Lấy số khách mới theo loại
    @GetMapping("/get-new-customer")
    public List<CustomerDTO> getNewCustomers(
            @RequestParam String dateStart,
            @RequestParam String dateEnd,
            @RequestParam String stockId
    ) throws Exception {
        return salesService.getNewCustomers(dateStart, dateEnd, stockId);
    }

//    @GetMapping("/get-new-customer-raw")
//    public ResponseEntity<String> getNewCustomersRaw(
//            @RequestParam String dateStart,
//            @RequestParam String dateEnd,
//            @RequestParam String stockId
//    ) throws Exception {
//        String json = salesService.getNewCustomersRaw(dateStart, dateEnd, stockId);
//        return ResponseEntity.ok(json);
//    }

    // Lấy số khách cũ theo loại
    @GetMapping("/get-old-customer")
    public List<CustomerDTO> getOldCustomers(
            @RequestParam String dateStart,
            @RequestParam String dateEnd,
            @RequestParam String stockId
    )  throws Exception {
        return salesService.getOldCustomers(dateStart, dateEnd, stockId);
    }

    // Lấy booking theo khung giờ (lọc theo Khách có đến với nguồn Khách mới + Khách cũ)
    @GetMapping("/get-booking-by-hour")
    public List<CustomerDTO> getBookingByHour(
            @RequestParam String dateStart,
            @RequestParam String dateEnd,
            @RequestParam String stockId
    )  throws Exception {
        return salesService.getAllBookingByHour(dateStart, dateEnd, stockId);
    }

    // Lấy đơn hàng theo khung giờ
    @GetMapping("/get-sales-by-hour")
    public List<Map<String, Object>> getSalesByHour(
            @RequestParam String dateStart,
            @RequestParam String dateEnd,
            @RequestParam String stockId
    ) throws Exception {
        return salesService.getSalesByHours(dateStart, dateEnd, stockId);
    }

    @GetMapping("/token-expiration")
    public ResponseEntity<?> checkTokenExpiration(@RequestHeader("Authorization") String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid token");
        }
        String token = header.substring(7);
        long remainingMs = jwtService.getTokenRemainingTime(token);
        return ResponseEntity.ok(Map.of(
                "remaining_ms", remainingMs,
                "remaining_minutes", remainingMs / 60000
        ));
    }
}
