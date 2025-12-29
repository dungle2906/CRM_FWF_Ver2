package com.example.basiccrmfwf.sales.application.usecase;

import com.example.basiccrmfwf.sales.application.dto.CustomerReportRequest;
import com.example.basiccrmfwf.sales.application.dto.TopCustomerSpendingDTO;
import com.example.basiccrmfwf.sales.domain.repository.SalesTransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetTopCustomersUseCase {
    
    private final SalesTransactionRepositoryPort repository;
    
    @Transactional(readOnly = true)
    public List<TopCustomerSpendingDTO> execute(CustomerReportRequest request) {
        List<Object[]> rawData = repository.fetchTopCustomersBySpending(
            request.getFromDate(), 
            request.getToDate()
        );
        
        return rawData.stream()
                .map(row -> new TopCustomerSpendingDTO(
                        (String) row[0], // phone number
                        (String) row[1], // customer name
                        (BigDecimal) row[2] // total spending
                ))
                .collect(Collectors.toList());
    }
}
