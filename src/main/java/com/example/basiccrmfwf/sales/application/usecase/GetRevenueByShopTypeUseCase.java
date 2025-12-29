package com.example.basiccrmfwf.sales.application.usecase;

import com.example.basiccrmfwf.sales.application.dto.CustomerReportRequest;
import com.example.basiccrmfwf.sales.application.dto.ShopTypeRevenueDTO;
import com.example.basiccrmfwf.sales.domain.repository.SalesTransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetRevenueByShopTypeUseCase {
    
    private final SalesTransactionRepositoryPort repository;
    
    @Transactional(readOnly = true)
    public List<ShopTypeRevenueDTO> execute(CustomerReportRequest request) {
        List<Object[]> rawData = repository.fetchRevenueByShopTypeAndDate(
            request.getFromDate(), 
            request.getToDate()
        );
        
        return rawData.stream()
                .map(row -> new ShopTypeRevenueDTO(
                        (String) row[0],
                        ((Date) row[1]).toLocalDate().atStartOfDay(),
                        (BigDecimal) row[2]))
                .collect(Collectors.toList());
    }
}
