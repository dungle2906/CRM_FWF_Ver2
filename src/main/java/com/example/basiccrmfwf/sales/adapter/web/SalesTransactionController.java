package com.example.basiccrmfwf.sales.adapter.web;

import com.example.basiccrmfwf.sales.application.dto.*;
import com.example.basiccrmfwf.sales.application.usecase.*;
import com.example.basiccrmfwf.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Web adapter (controller) for Sales Transaction domain.
 * Thin controller that delegates to use cases.
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CEO', 'TEAM_LEAD')")
public class SalesTransactionController {

    private final GetRevenueByRegionUseCase getRevenueByRegionUseCase;
    private final GetRevenueByShopTypeUseCase getRevenueByShopTypeUseCase;
    private final GetRevenueSummaryUseCase getRevenueSummaryUseCase;
    private final GetTopCustomersUseCase getTopCustomersUseCase;
    
    // TODO: Add other use cases as they are created
    // For now, we'll keep using the old service for endpoints not yet migrated

    @PostMapping("/region-revenue")
    public ResponseEntity<ApiResponse<List<RegionRevenueDTO>>> getRegionRevenue(
            @RequestBody CustomerReportRequest request) {
        List<RegionRevenueDTO> result = getRevenueByRegionUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/shop-type-revenue")
    public ResponseEntity<ApiResponse<List<ShopTypeRevenueDTO>>> getShopTypeRevenue(
            @RequestBody CustomerReportRequest request) {
        List<ShopTypeRevenueDTO> result = getRevenueByShopTypeUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/revenue-summary")
    public ResponseEntity<ApiResponse<RevenueSummaryDTO>> getRevenueSummary(
            @RequestBody CustomerReportRequest request) {
        RevenueSummaryDTO result = getRevenueSummaryUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/top-spenders")
    public ResponseEntity<ApiResponse<List<TopCustomerSpendingDTO>>> getTopSpenders(
            @RequestBody CustomerReportRequest request) {
        List<TopCustomerSpendingDTO> result = getTopCustomersUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    // TODO: Migrate remaining endpoints as use cases are created
    // For now, these will continue to use the old service through a compatibility layer
}
