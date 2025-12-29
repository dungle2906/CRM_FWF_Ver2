package com.example.basiccrmfwf.sales.adapter.persistence.repository;

import com.example.basiccrmfwf.sales.adapter.persistence.entity.SalesTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for SalesTransactionEntity.
 * Contains all the native queries from the original repository.
 */
public interface SalesTransactionJpaRepository extends JpaRepository<SalesTransactionEntity, Integer> {

    @Query("SELECT s.phoneNumber FROM SalesTransactionEntity s WHERE s.orderDate BETWEEN :from AND :to")
    List<String> findPhonesBetweenOrderDate(LocalDateTime from, LocalDateTime to);

    @Query("SELECT s.phoneNumber FROM SalesTransactionEntity s")
    List<String> findPhones();

    @Query("""
        select distinct st.phoneNumber
        from SalesTransactionEntity st
        where st.orderDate >= :start and st.orderDate <= :end
          and st.phoneNumber is not null
    """)
    List<String> findPhonesByOrderDateBetween(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    List<SalesTransactionEntity> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    @Query(value = """
        SELECT r.region AS region,
               DATE(s.order_date) AS date,
               SUM(s.cash_transfer_credit) AS totalRevenue
        FROM sales_transaction s
        JOIN region r ON s.facility_id = r.id
        WHERE s.order_date BETWEEN :start AND :end
        GROUP BY r.region, DATE(s.order_date)
        ORDER BY DATE(s.order_date)
    """, nativeQuery = true)
    List<Object[]> fetchRevenueByRegionAndDate(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT r.shop_type AS shopType,
               DATE(s.order_date) AS date,
               SUM(s.total_amount) AS totalRevenue
        FROM sales_transaction s
        JOIN region r ON s.facility_id = r.id
        WHERE s.order_date BETWEEN :start AND :end
        GROUP BY r.shop_type, DATE(s.order_date)
        ORDER BY DATE(s.order_date)
    """, nativeQuery = true)
    List<Object[]> fetchRevenueByShopTypeAndDate(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT SUM(s.prepaid_card)
        FROM sales_transaction s
        WHERE s.order_date BETWEEN :start AND :end
    """, nativeQuery = true)
    BigDecimal fetchRevenueSummary(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT SUM(s.cash_transfer_credit)
        FROM sales_transaction s
        WHERE s.order_date BETWEEN :start AND :end
    """, nativeQuery = true)
    BigDecimal fetchActualRevenueSummary(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT r.region AS region,
               COUNT(*) AS orders,
               SUM(s.cash_transfer_credit) AS revenue
        FROM sales_transaction s
        JOIN region r ON s.facility_id = r.id
        WHERE s.order_date BETWEEN :start AND :end
        GROUP BY r.region
    """, nativeQuery = true)
    List<Object[]> fetchOrderAndRevenueByRegion(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query(value = """
    SELECT r.region AS region,
           SUM(s.cash_transfer_credit) AS actualRevenue
    FROM sales_transaction s
    JOIN region r ON s.facility_id = r.id
    WHERE s.order_date BETWEEN :start AND :end
    GROUP BY r.region
""", nativeQuery = true)
    List<Object[]> fetchActualRevenueByRegion(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT DATE(s.order_date) AS order_date,
               r.region AS region,
               SUM(s.cash_transfer_credit) AS revenue
        FROM sales_transaction s
        JOIN region r ON s.facility_id = r.id
        WHERE s.order_date BETWEEN :start AND :end
        GROUP BY DATE(s.order_date), r.region
        ORDER BY DATE(s.order_date), r.region
    """, nativeQuery = true)
    List<Object[]> fetchDailyRevenueByRegion(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT DATE(st.order_date) AS order_date,
               r.shop_type AS shop_type,
               SUM(st.cash_transfer_credit) AS revenue
        FROM sales_transaction st
                 JOIN region r ON st.facility_id = r.id
        WHERE st.order_date BETWEEN :start AND :end
        GROUP BY DATE(st.order_date), r.shop_type
        ORDER BY DATE(st.order_date), r.shop_type
        """, nativeQuery = true)
    List<Object[]> getDailyRevenueByShopType(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query(value = "SELECT c.customer_type, DATE(s.order_date), SUM(s.cash_transfer_credit) " +
            "FROM customer_sale_record c " +
            "JOIN sales_transaction s ON c.phone_number = s.phone_number " +
            "WHERE s.order_date BETWEEN :start AND :end " +
            "GROUP BY c.customer_type, DATE(s.order_date)", nativeQuery = true)
    List<Object[]> findRevenueByCustomerTypeAndDate(@Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT r.shop_name,
               SUM(s.cash_transfer_credit) AS actualRevenue,
               SUM(s.prepaid_card) AS foxieCardRevenue
        FROM sales_transaction s
        JOIN region r ON s.facility_id = r.id
        WHERE s.order_date BETWEEN :start AND :end
        GROUP BY r.shop_name
        ORDER BY actualRevenue DESC
    """, nativeQuery = true)
    List<Object[]> findTop10StoreRevenue(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT
            r.shop_name,
            SUM(COALESCE(s.count_cb_dv, 0)) AS count_cb_dv,
            SUM(COALESCE(st.cash_transfer_credit, 0)) AS total_cash_transfer,
            SUM(COALESCE(st.prepaid_card, 0)) AS total_prepaid_card
        FROM
            sales_transaction st
        JOIN
            region r ON st.facility_id = r.id
        LEFT JOIN (
            SELECT ssi.sale_transaction_id, COUNT(*) AS count_cb_dv
            FROM sale_service_item ssi
            JOIN service_type ser ON ssi.service_type_id = ser.id
            WHERE ser.service_code LIKE 'CB%' OR ser.service_code LIKE 'DV%'
            GROUP BY ssi.sale_transaction_id
        ) s ON st.id = s.sale_transaction_id
        WHERE
            st.order_date BETWEEN :start AND :end
        GROUP BY
            r.shop_name
""", nativeQuery = true)
    List<Object[]> findStoreRevenueStatsBetween(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query(value = "SELECT DATE(order_date) as order_day, COUNT(*) as total_orders, COUNT(DISTINCT facility_id) as shop_count\n" +
            "FROM sales_transaction\n" +
            "WHERE order_date BETWEEN :start AND :end\n" +
            "GROUP BY DATE(order_date)\n" +
            "ORDER BY DATE(order_date)", nativeQuery = true)
    List<Object[]> findDailyOrderAndShopStats(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT
                r.shop_name AS shop_name,
                COUNT(DISTINCT st.id) AS total_orders,
                COUNT(CASE WHEN ser.service_code LIKE 'DV%' THEN 1 END) AS service_orders,
                COUNT(CASE WHEN st.prepaid_card > 0 THEN 1 END) AS cash_transfer_credit_orders,
                COUNT(CASE WHEN ser.service_code LIKE 'CB%' THEN 1 END) AS combo_orders,
                COUNT(CASE WHEN ser.category = 'Foxie Member Card' OR ser.service_name LIKE 'Foxie Card%' THEN 1 END) AS card_purchase_orders
            FROM sales_transaction st
            JOIN region r ON st.facility_id = r.id
            LEFT JOIN sale_service_item ssi ON st.id = ssi.sale_transaction_id
            LEFT JOIN service_type ser ON ssi.service_type_id = ser.id
            WHERE st.order_date BETWEEN :start AND :end
            GROUP BY r.shop_name
            ORDER BY total_orders DESC
    """, nativeQuery = true)
    List<Object[]> fetchRegionOrderBreakdown(LocalDateTime start, LocalDateTime end);

    @Query("SELECT st.facility.region, " +
            "       COALESCE(SUM(st.cash), 0), " +
            "       COALESCE(SUM(st.transfer), 0), " +
            "       COALESCE(SUM(st.creditCard), 0) " +
            "FROM SalesTransactionEntity st " +
            "WHERE st.orderDate BETWEEN :start AND :end " +
            "  AND st.cashTransferCredit > 0 " +
            "GROUP BY st.facility.region")
    List<Object[]> findPaymentByRegion(LocalDateTime start, LocalDateTime end);

    @Query(value = """
        SELECT
            COUNT(DISTINCT st.id) AS total_orders,
            COUNT(CASE WHEN ser.service_code LIKE 'CB%' OR ser.service_code LIKE 'DV%' THEN 1 END) AS service_combo_orders,
            COUNT(CASE WHEN st.prepaid_card > 0 THEN 1 END) AS foxie_card_orders,
            COUNT(CASE WHEN ser.service_code LIKE 'MD%' OR ser.service_code LIKE 'MP%' THEN 1 END) AS product_orders,
            COUNT(CASE WHEN ser.category LIKE 'Foxie Member Card' OR ser.service_name LIKE 'Foxie Card%' THEN 1 END) AS card_purchase_orders
        FROM sales_transaction st
        LEFT JOIN sale_service_item ssi ON st.id = ssi.sale_transaction_id
        LEFT JOIN service_type ser ON ssi.service_type_id = ser.id
        WHERE st.order_date BETWEEN :start AND :end
""", nativeQuery = true)
    List<Object[]> fetchOverallOrderSummary(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT
            COALESCE(SUM(st.total_amount), 0) AS total_revenue,
            COALESCE(SUM((service_type_flags.has_service = 1) * st.total_amount), 0) AS service_revenue,
            COALESCE(SUM((service_type_flags.has_card = 1) * st.cash_transfer_credit), 0) AS card_purchase_revenue,
            COALESCE(SUM((service_type_flags.has_product = 1) * st.cash_transfer_credit), 0) AS product_revenue,
            COALESCE(SUM(st.prepaid_card), 0) AS foxie_card_paid,
            COALESCE(SUM(st.cash_transfer_credit), 0) AS thucthu
    
        FROM sales_transaction st
        JOIN (
            SELECT
                ssi.sale_transaction_id,
                MAX(stt.category = 'Foxie Member Card' OR stt.service_name LIKE 'Foxie Card%') AS has_card,
                MAX(
                    (stt.service_code LIKE 'MD%' OR stt.service_code LIKE 'MP%')
                    AND NOT (stt.category = 'Foxie Member Card' OR stt.service_name LIKE 'Foxie Card%')
                ) AS has_product,
                MAX(
                    NOT (
                        stt.category = 'Foxie Member Card'
                        OR stt.service_name LIKE 'Foxie Card%'
                        OR stt.service_code LIKE 'MD%'
                        OR stt.service_code LIKE 'MP%'
                    )
                ) AS has_service
            FROM sale_service_item ssi
            JOIN service_type stt ON ssi.service_type_id = stt.id
            GROUP BY ssi.sale_transaction_id
        ) AS service_type_flags
            ON service_type_flags.sale_transaction_id = st.id
        WHERE st.order_date BETWEEN :start AND :end
    """, nativeQuery = true)
    List<Object[]> fetchOverallRevenueSummary(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query(value = """
       SELECT s.phone_number AS phoneNumber,
              s.customer_name AS customerName,
              SUM(s.cash_transfer_credit) AS totalSpending
       FROM sales_transaction s
       WHERE s.order_date BETWEEN :start AND :end
             AND s.phone_number IS NOT NULL
       GROUP BY s.phone_number, s.customer_name
       ORDER BY totalSpending DESC
       LIMIT 10
    """, nativeQuery = true)
    List<Object[]> fetchTopCustomersBySpending(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);
}
