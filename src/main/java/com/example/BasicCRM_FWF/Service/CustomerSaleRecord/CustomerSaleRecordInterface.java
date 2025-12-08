package com.example.BasicCRM_FWF.Service.CustomerSaleRecord;

import com.example.BasicCRM_FWF.DTORequest.CustomerReportRequest;
import com.example.BasicCRM_FWF.DTOResponse.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CustomerSaleRecordInterface {

    void importFromExcel(MultipartFile file);

    CustomerReportResponse getNewCustomerReport(CustomerReportRequest request);

    CustomerReportResponse getOldCustomerReport(CustomerReportRequest request);

    GenderRatioResponse getGenderRatio(CustomerReportRequest request);

    Map<String, List<DailyCountDTO>> getCustomerTypeTrend(CustomerReportRequest request);

    Map<String, List<DailyCountDTO>> getCustomerSourceTrend(CustomerReportRequest request);

    List<AppDownloadStatus> calculateAppDownloadStatus(CustomerReportRequest request);

    CustomerOrderSummaryDTO calculateAppDownloadSummary(LocalDateTime start, LocalDateTime end);

    CustomerSummaryDTO calculateCustomerSummary(CustomerReportRequest request);

    List<DailyCustomerOrderTrendDTO> calculateCustomerOrderTrends(LocalDateTime start, LocalDateTime end);

    CustomerOrderSummaryDTO calculateCustomerOrderSummary(LocalDateTime start, LocalDateTime end);

    CustomerOrderSummaryDTO calculateGenderSummary(LocalDateTime start, LocalDateTime end);

    GenderRevenueDTO calculateGenderRevenue(LocalDateTime start, LocalDateTime end);

    PaymentBreakdownDTO calculatePaymentStatus(CustomerReportRequest request, boolean isNew);

    TotalCustomerResponse getCustomerSaleRecord(CustomerReportRequest request);

    long countUniquePhonesBetweenRange(CustomerReportRequest request);

    long countUniquePhones();

    List<HourlyFacilityStatsDTO> getHourlyStats(LocalDateTime start, LocalDateTime end);

}
