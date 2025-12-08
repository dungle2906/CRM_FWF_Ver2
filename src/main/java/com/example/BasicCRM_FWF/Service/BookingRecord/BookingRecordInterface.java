package com.example.BasicCRM_FWF.Service.BookingRecord;

import com.example.BasicCRM_FWF.DTOResponse.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRecordInterface {

    public void importFromExcel(MultipartFile file);

    public List<HourlyFacilityStatsDTO> getHourlyArrivalStats(CustomerReportRequestVer2 request);

    public List<BookingStatusStatsDTO> getBookingStatusStats(LocalDateTime start, LocalDateTime end);

    public CustomerStatusRatioDTO getCustomerStatusRatio(LocalDateTime start, LocalDateTime end);

    public List<TopCustomerDTO> getTopCustomers(LocalDateTime start, LocalDateTime end);

    public List<TopEmployeeDTO> getTopBookingEmployee(LocalDateTime start, LocalDateTime end);
}
