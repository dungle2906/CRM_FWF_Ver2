package com.example.basiccrmfwf.sales.application.usecase;

import com.example.basiccrmfwf.sales.application.dto.CustomerReportRequest;
import com.example.basiccrmfwf.sales.application.dto.RevenueSummaryDTO;
import com.example.basiccrmfwf.sales.domain.repository.SalesTransactionRepositoryPort;
import com.example.basiccrmfwf.shared.util.ServiceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Use case for getting revenue summary with growth calculation.
 */
@Service
@RequiredArgsConstructor
public class GetRevenueSummaryUseCase {
    
    private final SalesTransactionRepositoryPort repository;
    
    @Transactional(readOnly = true)
    public RevenueSummaryDTO execute(CustomerReportRequest request) {
        DateRangeResult dateRange = calculateDateRange(request);
        
        BigDecimal total = repository.fetchRevenueSummary(dateRange.fromDate(), dateRange.toDate());
        BigDecimal actual = repository.fetchActualRevenueSummary(dateRange.fromDate(), dateRange.toDate());
        
        BigDecimal prevTotal = repository.fetchRevenueSummary(dateRange.prevFrom(), dateRange.prevTo());
        BigDecimal prevActual = repository.fetchActualRevenueSummary(dateRange.prevFrom(), dateRange.prevTo());
        
        double growthTotal = ServiceUtils.calculateGrowthBigDecimal(prevTotal, total);
        double growthActual = ServiceUtils.calculateGrowthBigDecimal(prevActual, actual);
        
        return new RevenueSummaryDTO(total, actual, growthTotal, growthActual);
    }
    
    /**
     * Calculate date range and previous period (similar to CustomerSaleRecordService.getResult).
     */
    private DateRangeResult calculateDateRange(CustomerReportRequest request) {
        // Normalize time boundaries
        LocalDateTime fromDate = request.getFromDate().with(LocalTime.MIN);
        LocalDateTime toDate = request.getToDate().with(LocalTime.MAX);
        
        LocalDateTime prevFrom;
        LocalDateTime prevTo;
        
        if (ServiceUtils.isWholeMonthSelection(fromDate, toDate)) {
            // Month-over-month comparison
            LocalDate firstOfCurrentMonth = fromDate.toLocalDate().withDayOfMonth(1);
            LocalDate firstOfPrevMonth = firstOfCurrentMonth.minusMonths(1);
            LocalDate lastOfPrevMonth = firstOfPrevMonth.with(TemporalAdjusters.lastDayOfMonth());
            
            prevFrom = firstOfPrevMonth.atTime(LocalTime.MIN);
            prevTo = lastOfPrevMonth.atTime(LocalTime.MAX);
        } else {
            // Day range: shift back by the same number of days
            long daysBetween = ChronoUnit.DAYS.between(fromDate.toLocalDate(), toDate.toLocalDate()) + 1;
            prevFrom = fromDate.minusDays(daysBetween);
            prevTo = toDate.minusDays(daysBetween);
        }
        
        return new DateRangeResult(fromDate, toDate, prevFrom, prevTo);
    }
    
    private record DateRangeResult(
        LocalDateTime fromDate,
        LocalDateTime toDate,
        LocalDateTime prevFrom,
        LocalDateTime prevTo
    ) {}
}
