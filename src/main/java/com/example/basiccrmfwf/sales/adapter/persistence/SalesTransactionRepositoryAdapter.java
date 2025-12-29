package com.example.basiccrmfwf.sales.adapter.persistence;

import com.example.basiccrmfwf.sales.domain.model.SalesTransaction;
import com.example.basiccrmfwf.sales.domain.repository.SalesTransactionRepositoryPort;
import com.example.basiccrmfwf.sales.adapter.persistence.entity.SalesTransactionEntity;
import com.example.basiccrmfwf.sales.adapter.persistence.mapper.SalesTransactionEntityMapper;
import com.example.basiccrmfwf.sales.adapter.persistence.repository.SalesTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persistence adapter that implements the domain repository port.
 * This adapter bridges between domain models and JPA entities.
 */
@Component
@RequiredArgsConstructor
public class SalesTransactionRepositoryAdapter implements SalesTransactionRepositoryPort {
    
    private final SalesTransactionJpaRepository jpaRepository;
    private final SalesTransactionEntityMapper mapper;
    
    @Override
    public SalesTransaction save(SalesTransaction transaction) {
        SalesTransactionEntity entity = mapper.toEntity(transaction);
        SalesTransactionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public List<SalesTransaction> findByOrderDateBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByOrderDateBetween(start, end).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<String> findPhonesBetweenOrderDate(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findPhonesBetweenOrderDate(from, to);
    }
    
    @Override
    public List<String> findPhones() {
        return jpaRepository.findPhones();
    }
    
    @Override
    public List<String> findPhonesByOrderDateBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findPhonesByOrderDateBetween(start, end);
    }
    
    @Override
    public List<Object[]> fetchRevenueByRegionAndDate(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.fetchRevenueByRegionAndDate(start, end);
    }
    
    @Override
    public List<Object[]> fetchRevenueByShopTypeAndDate(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.fetchRevenueByShopTypeAndDate(start, end);
    }
    
    @Override
    public BigDecimal fetchRevenueSummary(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.fetchRevenueSummary(start, end);
    }
    
    @Override
    public BigDecimal fetchActualRevenueSummary(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.fetchActualRevenueSummary(start, end);
    }
    
    @Override
    public List<Object[]> fetchOrderAndRevenueByRegion(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.fetchOrderAndRevenueByRegion(start, end);
    }
    
    @Override
    public List<Object[]> fetchActualRevenueByRegion(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.fetchActualRevenueByRegion(start, end);
    }
    
    @Override
    public List<Object[]> fetchDailyRevenueByRegion(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.fetchDailyRevenueByRegion(start, end);
    }
    
    @Override
    public List<Object[]> getDailyRevenueByShopType(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.getDailyRevenueByShopType(start, end);
    }
    
    @Override
    public List<Object[]> findRevenueByCustomerTypeAndDate(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findRevenueByCustomerTypeAndDate(start, end);
    }
    
    @Override
    public List<Object[]> findTop10StoreRevenue(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findTop10StoreRevenue(start, end);
    }
    
    @Override
    public List<Object[]> findStoreRevenueStatsBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findStoreRevenueStatsBetween(start, end);
    }
    
    @Override
    public List<Object[]> findDailyOrderAndShopStats(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findDailyOrderAndShopStats(start, end);
    }
    
    @Override
    public List<Object[]> findPaymentByRegion(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findPaymentByRegion(start, end);
    }
    
    @Override
    public List<Object[]> fetchRegionOrderBreakdown(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.fetchRegionOrderBreakdown(start, end);
    }
    
    @Override
    public List<Object[]> fetchOverallOrderSummary(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.fetchOverallOrderSummary(start, end);
    }
    
    @Override
    public List<Object[]> fetchOverallRevenueSummary(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.fetchOverallRevenueSummary(start, end);
    }
    
    @Override
    public List<Object[]> fetchTopCustomersBySpending(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.fetchTopCustomersBySpending(start, end);
    }
}
