package com.example.BasicCRM_FWF.Controller;

import com.example.BasicCRM_FWF.DTO.FullDateRangeResponse;
import com.example.BasicCRM_FWF.DTO.PhoneExportDTO;
import com.example.BasicCRM_FWF.DTORequest.CustomerReportRequest;
import com.example.BasicCRM_FWF.DTOResponse.*;
import com.example.BasicCRM_FWF.Service.CustomerSaleRecord.CustomerSaleRecordInterface;
import com.example.BasicCRM_FWF.Service.CustomerSaleRecord.CustomerSaleRecordService;
import com.example.BasicCRM_FWF.Service.FullDateRangeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// BÁO CÁO KHÁCH
@RestController
@RequestMapping("/api/customer-sale")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CEO', 'TEAM_LEAD')")
// danh sach ban hang
public class CustomerSaleRecordController {

    private final CustomerSaleRecordInterface service;
    private final FullDateRangeService fullDateRangeService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        service.importFromExcel(file);
        return ResponseEntity.ok("Upload completed. Check logs for details." + file.getOriginalFilename());
    }

    // Trả về API thông báo select range time hợp lệ
    @GetMapping("/range-time-alert")
    public ResponseEntity<FullDateRangeResponse> getFullSystemDateRange() {
        return ResponseEntity.ok(fullDateRangeService.getFullRange());
    }

    // line chart số khách tạo mới
    @PostMapping("/new-customer-lineChart")
    public ResponseEntity<CustomerReportResponse> getNewCustomerReport(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getNewCustomerReport(request));
    }

    // line chart số khách cũ (KH Thành viên)
    @PostMapping("/old-customer-lineChart")
    public ResponseEntity<CustomerReportResponse> getOldCustomerReport(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getOldCustomerReport(request));
    }

    // Pie chart tỉ lệ nam nữ
    @PostMapping("/gender-ratio")
    public ResponseEntity<GenderRatioResponse> getGenderRatio(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getGenderRatio(request));
    }

    // 2 tab tổng số kh mới trong hệ thống + tổng số khách mới thực đi
    @PostMapping("/customer-summary")
    public ResponseEntity<CustomerSummaryDTO> getCustomerSummary(@RequestBody CustomerReportRequest request) {
        CustomerSummaryDTO summary = service.calculateCustomerSummary(request);
        return ResponseEntity.ok(summary);
    }

    // Số KH tới chia theo loại
    @PostMapping("/customer-type-trend")
    public ResponseEntity<Map<String, List<DailyCountDTO>>> getCustomerTypeTrend(
            @RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getCustomerTypeTrend(request));
    }

    // Nguồn KH đến từ
    @PostMapping("/customer-source-trend")
    public ResponseEntity<Map<String, List<DailyCountDTO>>> getCustomerSourceTrend(
            @RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getCustomerSourceTrend(request));
    }

    // Tỉ lệ tải app/ không tải chart
    @PostMapping("/app-download-status")
    public ResponseEntity<List<AppDownloadStatus>> getAppDownloadStats(
            @RequestBody CustomerReportRequest request
    ) {
        return ResponseEntity.ok(service.calculateAppDownloadStatus(request));
    }

    @PostMapping("/customer-old-new-order-trends")
    public ResponseEntity<List<DailyCustomerOrderTrendDTO>> getTrends(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.calculateCustomerOrderTrends(request.getFromDate(), request.getToDate()));
    }

    @PostMapping("/customer-old-new-order-pieChart")
    public CustomerOrderSummaryDTO getSummary(@RequestBody CustomerReportRequest request) {
        return service.calculateCustomerOrderSummary(request.getFromDate(), request.getToDate());
    }

    // Tỉ lệ tải app
    @PostMapping("/app-download-pieChart")
    public CustomerOrderSummaryDTO getAppDownloadSummary(@RequestBody CustomerReportRequest request) {
        return service.calculateAppDownloadSummary(request.getFromDate(), request.getToDate());
    }

    // Tỉ lệ nam nữ
    @PostMapping("/gender-distribution")
    public CustomerOrderSummaryDTO getGenderSummary(@RequestBody CustomerReportRequest request) {
        return service.calculateGenderSummary(request.getFromDate(), request.getToDate());
    }

    // Tính thực thu theo loại KH nam/nữ
    @PostMapping("/gender-revenue")
    public GenderRevenueDTO getGenderRevenue(@RequestBody CustomerReportRequest request) {
        return service.calculateGenderRevenue(request.getFromDate(), request.getToDate());
    }

    // Tỉ lệ hình thức thanh toán khách cũ (TAB kế toán)
    @PostMapping("/payment-percent-new")
    public PaymentBreakdownDTO getNewCustomerPayments(@RequestBody CustomerReportRequest request) {
        return service.calculatePaymentStatus(request, true);
    }

    // Tỉ lệ hình thức thanh toán khách mới (TAB kế toán)
    @PostMapping("/payment-percent-old")
    public PaymentBreakdownDTO getOldCustomerPayments(@RequestBody CustomerReportRequest request) {
        return service.calculatePaymentStatus(request, false);
    }

    // Tổng số lượt khách sử dụng dịch vụ trong ngày đã chọn
    @PostMapping("/unique-customers-comparison")
    public TotalCustomerResponse compareUniqueCustomers(@RequestBody CustomerReportRequest request) {
        return service.getCustomerSaleRecord(request);
    }

    // Lấy tổng số khách của FWF trong khoảng thời gian
    @PostMapping("/get-all-customer")
    public ResponseEntity<Long> getAllCustomer(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.countUniquePhonesBetweenRange(request));
    }

    // Lấy tổng số khách của FWF từ trước tới giờ
    @GetMapping("/get-all-customer-no-range-time")
    public ResponseEntity<Long> getAllCustomerNoRangeTime() {
        return ResponseEntity.ok(service.countUniquePhones());
    }

    @PostMapping(
            value = "/export-customer-phones",
            produces = "text/csv"
    )
    public void exportCustomerPhonesToCsv(HttpServletResponse response) throws IOException {

        List<PhoneExportDTO> data = service.exportFullPhoneNumbers();

        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=customer_phones.csv"
        );

        try (PrintWriter writer = response.getWriter()) {

            writer.println("STT,PHONE,SOURCE");

            int index = 1;
            for (PhoneExportDTO dto : data) {
                writer.println(index++ + "," + dto.getPhone() + "," + dto.getSource());
            }
        }
    }

    // Thời gian đơn hàng được tạo
    @PostMapping("/facility-hour-service")
    public ResponseEntity<List<HourlyFacilityStatsDTO>> getHourlyReport(@RequestBody CustomerReportRequest request) {
        LocalDateTime start = request.getFromDate();
        LocalDateTime end = request.getToDate();

        List<HourlyFacilityStatsDTO> stats = service.getHourlyStats(start, end);
        return ResponseEntity.ok(stats);
    }
}
