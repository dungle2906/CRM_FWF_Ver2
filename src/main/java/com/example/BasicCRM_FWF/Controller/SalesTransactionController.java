package com.example.BasicCRM_FWF.Controller;

import com.example.BasicCRM_FWF.DTORequest.CustomerReportRequest;
import com.example.BasicCRM_FWF.DTOResponse.*;
import com.example.BasicCRM_FWF.Service.SalesTransaction.SalesTransactionInterface;
import com.example.BasicCRM_FWF.Service.SalesTransaction.SalesTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// BÁO CÁO DOANH SỐ
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CEO', 'TEAM_LEAD')")
// Ban hang doanh so
public class SalesTransactionController {

    private final SalesTransactionInterface service;

//    @PostMapping("/upload-temp")
//    public ResponseEntity<String> uploadTemp(@RequestParam("file") MultipartFile file) throws IOException {
//        service.importFromExcelTestChange(file);
//        return ResponseEntity.ok("Upload successful" + file.getOriginalFilename());
//    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        service.importFromExcel(file);
        return ResponseEntity.ok("Upload successful" + file.getOriginalFilename());
    }

//    // Tổng doanh số vùng
    @PostMapping("/region-revenue")
    public ResponseEntity<List<RegionRevenueDTO>> getRegionRevenue(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getRevenueByRegion(request));
    }

//    // Tổng doanh số cửa hàng
    @PostMapping("/shop-type-revenue")
    public ResponseEntity<List<ShopTypeRevenueDTO>> getShopTypeRevenue(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getRevenueByShopType(request));
    }

    // Tổng doanh số + tổng thực thu tab
    @PostMapping("/revenue-summary")
    public ResponseEntity<RevenueSummaryDTO> getRevenueSummary(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getRevenueSummary(request));
    }

    // Thực thu của các khu vực trong range time
    @PostMapping("/region-stat")
    public ResponseEntity<List<RegionRevenueStatDTO>> getRegionStat(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getStatus(request));
    }

    // Pie chart thực thu các khu vực trong range time
    @PostMapping("/region-actual-pie")
    public ResponseEntity<List<RegionRevenuePieDTO>> getPie(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getActualRevenuePie(request));
    }

    // Tổng thực thu theo loại cửa hàng
    @PostMapping("/daily-by-shop-type")
    public List<DailyShopTypeRevenueDTO> getDailyRevenueByShopType(@RequestBody CustomerReportRequest request) {
        return service.getDailyRevenueByShopType(request);
    }

    // Tổng thực thu theo loại khách hàng trong range time
    @PostMapping("/daily-by-customer-type")
    public ResponseEntity<List<DailyCustomerTypeRevenueDTO>> getDailyRevenueByCustomerType(
            @RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getRevenueByCustomerTypePerDay(request));
    }

    // Top 10 cửa hàng trong range time thực thu
    @PostMapping("/top-store-revenue")
    public List<TopStoreRevenueDTO> topStoreRevenue(@RequestBody CustomerReportRequest request) {
        return service.getTopStoreRevenue(request);
    }

    // Thực thu cửa hàng (KPI cửa hàng)
    @PostMapping("/full-store-revenue")
    public List<StoreRevenueStatDTO> getFullStoreRevenue(@RequestBody CustomerReportRequest request) {
        return service.getFullStoreRevenueStats(request);
    }

    // Số lượng đơn hàng theo ngày (-đơn mua thẻ)
    @PostMapping("/daily-order-stats")
    public List<DailyShopOrderStatDTO> getDailyOrderStats(@RequestBody CustomerReportRequest request) {
        return service.getDailyOrderStats(request);
    }

    // Tổng thực thu tại các khu vực theo ngày
    @PostMapping("/daily-region-revenue")
    public ResponseEntity<List<DailyRegionRevenueDTO>> getDailyRevenue(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getDailyRevenue(request));
    }

    // Hình thức thanh toán theo vùng
    @PostMapping("/payment-by-region")
    public ResponseEntity<List<RegionPaymentDTO>> getPaymentByRegion(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getPaymentByRegion(request));
    }

    // Top 10 cửa hàng theo đơn hàng
    @PostMapping("/region-order-breakdown")
    public ResponseEntity<List<RegionOrderBreakdownDTO>> getRegionOrderBreakdown(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getRegionOrderBreakdown(request));
    }

    // KPI Cửa hàng (Số đơn tại các cửa hàng)
    @PostMapping("/region-order-breakdown-table")
    public ResponseEntity<List<RegionOrderBreakdownTableDTO>> getRegionOrderBreakdownTable(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getRegionOrderBreakdownTable(request));
    }

    // 5 Tab ô nhỏ cửa overall summary
    @PostMapping("/overall-order-summary")
    public ResponseEntity<OverallOrderSummaryDTO> getOverallOrderSummary(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getOverallOrderSummary(request));
    }

    // 6 Tab doanh thu
    @PostMapping("/overall-summary")
    public ResponseEntity<OverallSummaryDTO> getOverallSummary(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getOverallSummary(request));
    }

    // Top 10 KH chi mạnh tay nhất
    @PostMapping("/top-spenders")
    public ResponseEntity<List<TopCustomerSpendingDTO>> getTopSpenders(@RequestBody CustomerReportRequest request) {
        return ResponseEntity.ok(service.getTopCustomersBySpending(request));
    }
}
