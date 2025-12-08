package com.example.BasicCRM_FWF.Service.SalesTransaction;

import com.example.BasicCRM_FWF.DTORequest.CustomerReportRequest;
import com.example.BasicCRM_FWF.DTOResponse.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SalesTransactionInterface {
    public void importFromExcel(MultipartFile file);

    public List<RegionRevenueDTO> getRevenueByRegion(CustomerReportRequest request);

    public List<ShopTypeRevenueDTO> getRevenueByShopType(CustomerReportRequest request);

    public RevenueSummaryDTO getRevenueSummary(CustomerReportRequest request);

    public List<RegionRevenueStatDTO> getStatus(CustomerReportRequest request);

    public List<RegionRevenuePieDTO> getActualRevenuePie(CustomerReportRequest request);

    public List<DailyShopTypeRevenueDTO> getDailyRevenueByShopType(CustomerReportRequest request);

    public List<DailyCustomerTypeRevenueDTO> getRevenueByCustomerTypePerDay(CustomerReportRequest request);

    public List<TopStoreRevenueDTO> getTopStoreRevenue(CustomerReportRequest request);

    public List<StoreRevenueStatDTO> getFullStoreRevenueStats(CustomerReportRequest request);

    public List<DailyShopOrderStatDTO> getDailyOrderStats(CustomerReportRequest request);

    public List<DailyRegionRevenueDTO> getDailyRevenue(CustomerReportRequest request);

    public List<RegionPaymentDTO> getPaymentByRegion(CustomerReportRequest request);

    public List<RegionOrderBreakdownDTO> getRegionOrderBreakdown(CustomerReportRequest request);

    public List<RegionOrderBreakdownTableDTO> getRegionOrderBreakdownTable(CustomerReportRequest request);

    public OverallOrderSummaryDTO getOverallOrderSummary(CustomerReportRequest request);

    public OverallSummaryDTO getOverallSummary(CustomerReportRequest request);

    public List<TopCustomerSpendingDTO> getTopCustomersBySpending(CustomerReportRequest request);

}
