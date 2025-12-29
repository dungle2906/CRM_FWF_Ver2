package com.example.basiccrmfwf.sales.domain.repository;

import com.example.basiccrmfwf.sales.domain.model.SalesTransaction;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository port for SalesTransaction domain.
 * This is a pure interface - no Spring/JPA dependencies.
 * Implementation will be provided by persistence adapter.
 */
public interface SalesTransactionRepositoryPort {
    
    /**
     * Save a sales transaction.
     */
    SalesTransaction save(SalesTransaction transaction);
    
    /**
     * Find transactions by order date range.
     */
    List<SalesTransaction> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find phone numbers between order dates.
     */
    List<String> findPhonesBetweenOrderDate(LocalDateTime from, LocalDateTime to);
    
    /**
     * Find all phone numbers.
     */
    List<String> findPhones();
    
    /**
     * Find phone numbers by order date between.
     */
    List<String> findPhonesByOrderDateBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Fetch revenue by region and date (returns raw Object[] for complex queries).
     * Note: This method returns Object[] because it's a complex aggregation query.
     * In a pure DDD approach, we might create a value object for this, but for now
     * we'll keep it simple and handle transformation in the use case.
     */
    List<Object[]> fetchRevenueByRegionAndDate(LocalDateTime start, LocalDateTime end);
    
    /**
     * Fetch revenue by shop type and date.
     */
    List<Object[]> fetchRevenueByShopTypeAndDate(LocalDateTime start, LocalDateTime end);
    
    /**
     * Fetch revenue summary (total).
     */
    java.math.BigDecimal fetchRevenueSummary(LocalDateTime start, LocalDateTime end);
    
    /**
     * Fetch actual revenue summary (cash + transfer + credit).
     */
    java.math.BigDecimal fetchActualRevenueSummary(LocalDateTime start, LocalDateTime end);
    
    /**
     * Fetch order and revenue by region.
     */
    List<Object[]> fetchOrderAndRevenueByRegion(LocalDateTime start, LocalDateTime end);
    
    /**
     * Fetch actual revenue by region.
     */
    List<Object[]> fetchActualRevenueByRegion(LocalDateTime start, LocalDateTime end);
    
    /**
     * Fetch daily revenue by region.
     */
    List<Object[]> fetchDailyRevenueByRegion(LocalDateTime start, LocalDateTime end);
    
    /**
     * Get daily revenue by shop type.
     */
    List<Object[]> getDailyRevenueByShopType(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find revenue by customer type and date.
     */
    List<Object[]> findRevenueByCustomerTypeAndDate(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find top 10 store revenue.
     */
    List<Object[]> findTop10StoreRevenue(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find store revenue stats between dates.
     */
    List<Object[]> findStoreRevenueStatsBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find daily order and shop stats.
     */
    List<Object[]> findDailyOrderAndShopStats(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find payment by region.
     */
    List<Object[]> findPaymentByRegion(LocalDateTime start, LocalDateTime end);
    
    /**
     * Fetch region order breakdown.
     */
    List<Object[]> fetchRegionOrderBreakdown(LocalDateTime start, LocalDateTime end);
    
    /**
     * Fetch overall order summary.
     */
    List<Object[]> fetchOverallOrderSummary(LocalDateTime start, LocalDateTime end);
    
    /**
     * Fetch overall revenue summary.
     */
    List<Object[]> fetchOverallRevenueSummary(LocalDateTime start, LocalDateTime end);
    
    /**
     * Fetch top customers by spending.
     */
    List<Object[]> fetchTopCustomersBySpending(LocalDateTime start, LocalDateTime end);
}
