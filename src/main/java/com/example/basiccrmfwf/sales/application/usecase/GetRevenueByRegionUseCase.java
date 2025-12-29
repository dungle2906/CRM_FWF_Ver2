package com.example.basiccrmfwf.sales.application.usecase;

import com.example.basiccrmfwf.sales.application.dto.CustomerReportRequest;
import com.example.basiccrmfwf.sales.application.dto.RegionRevenueDTO;
import com.example.basiccrmfwf.sales.domain.repository.SalesTransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for getting revenue by region.
 */
@Service
@RequiredArgsConstructor
public class GetRevenueByRegionUseCase {
    
    private final SalesTransactionRepositoryPort repository;
    
    @Transactional(readOnly = true)
    public List<RegionRevenueDTO> execute(CustomerReportRequest request) {
        List<Object[]> rawData = repository.fetchRevenueByRegionAndDate(
            request.getFromDate(), 
            request.getToDate()
        );
        
        return rawData.stream()
                .map(row -> new RegionRevenueDTO(
                        (String) row[0],
                        ((Date) row[1]).toLocalDate(),
                        (BigDecimal) row[2]))
                .collect(Collectors.toList());
    }
}
