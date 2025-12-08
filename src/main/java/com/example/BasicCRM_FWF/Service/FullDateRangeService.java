package com.example.BasicCRM_FWF.Service;

import com.example.BasicCRM_FWF.DTO.FullDateRangeResponse;
import com.example.BasicCRM_FWF.Repository.CustomerSaleRecordRepository;
import com.example.BasicCRM_FWF.Service.Realtime.RealTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FullDateRangeService {

    private final CustomerSaleRecordRepository repo;

    public FullDateRangeResponse getFullRange() {

        FullDateRangeService.FullDateRangeInfo info = repo.getFullRange();

        // Gộp min
        LocalDateTime globalMin = Stream.of(
                info.getMinCustomerCreatedAt(),
                info.getMinBookingCreatedDate(),
                info.getMinSalesOrderDate(),
                info.getMinServiceBookingDate()
        ).filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);

        // Gộp max
        LocalDateTime globalMax = Stream.of(
                info.getMaxCustomerCreatedAt(),
                info.getMaxBookingCreatedDate(),
                info.getMaxSalesOrderDate(),
                info.getMaxServiceBookingDate()
        ).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);

        // Maps detail cho frontend muốn show bảng
        Map<String, LocalDateTime> minMap = new LinkedHashMap<>();
        minMap.put("customer_sale_record", info.getMinCustomerCreatedAt());
        minMap.put("booking_record", info.getMinBookingCreatedDate());
        minMap.put("sales_transaction", info.getMinSalesOrderDate());
        minMap.put("service_record", info.getMinServiceBookingDate());

        Map<String, LocalDateTime> maxMap = new LinkedHashMap<>();
        maxMap.put("customer_sale_record", info.getMaxCustomerCreatedAt());
        maxMap.put("booking_record", info.getMaxBookingCreatedDate());
        maxMap.put("sales_transaction", info.getMaxSalesOrderDate());
        maxMap.put("service_record", info.getMaxServiceBookingDate());

        FullDateRangeResponse result = new FullDateRangeResponse();
        result.setGlobalMin(globalMin);
        result.setGlobalMax(globalMax);
        result.setMinMap(minMap);
        result.setMaxMap(maxMap);

        result.setMessage(
                "Bạn nên chọn khoảng thời gian từ " +
                globalMin.toLocalDate() +
                " đến " +
                globalMax.toLocalDate() +
                " để tránh lỗi dữ liệu không tồn tại trong CRM."
        );

        return result;
    }

    public interface FullDateRangeInfo {
        LocalDateTime getMaxCustomerCreatedAt();
        LocalDateTime getMaxBookingCreatedDate();
        LocalDateTime getMaxSalesOrderDate();
        LocalDateTime getMaxServiceBookingDate();

        LocalDateTime getMinCustomerCreatedAt();
        LocalDateTime getMinBookingCreatedDate();
        LocalDateTime getMinSalesOrderDate();
        LocalDateTime getMinServiceBookingDate();
    }
}
