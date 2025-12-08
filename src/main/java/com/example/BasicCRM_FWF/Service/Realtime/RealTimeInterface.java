package com.example.BasicCRM_FWF.Service.Realtime;

import com.example.BasicCRM_FWF.DTORealTime.*;

import java.util.List;
import java.util.Map;

public interface RealTimeInterface {
    SalesSummaryDTO getSales(String dateStart, String dateEnd, String stockId) throws Exception;

    SalesSummaryDTO getSalesCopied(String dateStart, String dateEnd, String stockId) throws Exception;

    String getActualRevenue(String dateStart, String dateEnd, String stockId) throws Exception;

    ServiceSummaryDTO getServiceSummary(String dateStart, String dateEnd, String stockId) throws Exception;

    List<ServiceItems> getTop10Service(String dateStart, String dateEnd, String stockId) throws Exception;

    List<SalesDetailDTO> getSalesDetail(String dateStart, String dateEnd) throws Exception;

    BookingDTO getBookings(String dateStart, String dateEnd, String stockId) throws Exception;

    List<CustomerDTO> getNewCustomers(String dateStart, String dateEnd, String stockId) throws Exception;

    String getNewCustomersRaw(String dateStart, String dateEnd, String stockId) throws Exception;

    List<CustomerDTO> getOldCustomers(String dateStart, String dateEnd, String stockId) throws Exception;

    List<CustomerDTO> getAllBookingByHour(String dateStart, String dateEnd, String stockId) throws Exception;

    List<Map<String, Object>> getSalesByHours(String dateStart, String dateEnd, String stockId) throws Exception;

    void autoSaveWorkTrack() throws Exception;

//    public List<CustomerDTO> getCustomersByType(String dateStart, String dateEnd, String type) throws Exception;
}
