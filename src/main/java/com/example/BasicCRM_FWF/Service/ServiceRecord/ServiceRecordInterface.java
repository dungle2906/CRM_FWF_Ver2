package com.example.BasicCRM_FWF.Service.ServiceRecord;

import com.example.BasicCRM_FWF.DTORequest.CustomerReportRequest;
import com.example.BasicCRM_FWF.DTOResponse.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ServiceRecordInterface {
    void importFromExcelOrigin(MultipartFile file);

    void importSaleServiceFile(MultipartFile file);

    List<DailyServiceTypeStatDTO> getServiceTypeBreakdown(CustomerReportRequest request);

    ServiceSummaryDTO getServiceSummary(CustomerReportRequest request);

    List<RegionServiceTypeUsageDTO> getServiceUsageByRegion(CustomerReportRequest request);

    List<ServiceUsageDTO> getServiceUsageByShop(CustomerReportRequest request);

    List<TopServiceUsage> getTop10ServiceUsage(CustomerReportRequest request);

    List<TopServiceRevenue> getTop10ServicesByRevenue(CustomerReportRequest request);

    List<TopServiceRevenue> getBottom3ServiceRevenue(CustomerReportRequest request);

    List<TopServiceUsage> getBottom3ServicesUsage(CustomerReportRequest request);

    List<ServiceStatsDTO> getTopServiceTable(CustomerReportRequest request);
}
