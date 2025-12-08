package com.example.BasicCRM_FWF.Repository;

import com.example.BasicCRM_FWF.DTOResponse.DailyCustomerCount;
import com.example.BasicCRM_FWF.Model.CustomerSaleRecord;
import com.example.BasicCRM_FWF.Service.FullDateRangeService;
import com.example.BasicCRM_FWF.Service.Realtime.RealTimeService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerSaleRecordRepository extends JpaRepository<CustomerSaleRecord, Integer> {

    @Query("SELECT c.phoneNumber FROM CustomerSaleRecord c WHERE c.createdAt BETWEEN :from AND :to")
    List<String> findPhonesBetweenCreatedAt(LocalDateTime from, LocalDateTime to);

    @Query("SELECT c.phoneNumber FROM CustomerSaleRecord c")
    List<String> findPhones();

    @Query(value = """
        SELECT
            (SELECT MAX(created_at)   FROM temp_crm.customer_sale_record) AS maxCustomerCreatedAt,
            (SELECT MAX(created_date) FROM temp_crm.booking_record)       AS maxBookingCreatedDate,
            (SELECT MAX(order_date)   FROM temp_crm.sales_transaction)    AS maxSalesOrderDate,
            (SELECT MAX(booking_date) FROM temp_crm.service_record)       AS maxServiceBookingDate,

            (SELECT MIN(created_at)   FROM temp_crm.customer_sale_record) AS minCustomerCreatedAt,
            (SELECT MIN(created_date) FROM temp_crm.booking_record)       AS minBookingCreatedDate,
            (SELECT MIN(order_date)   FROM temp_crm.sales_transaction)    AS minSalesOrderDate,
            (SELECT MIN(booking_date) FROM temp_crm.service_record)       AS minServiceBookingDate
        """,
            nativeQuery = true)
    FullDateRangeService.FullDateRangeInfo getFullRange();

    @Query(value = """
        WITH ranked AS (
            SELECT *,
                   ROW_NUMBER() OVER (PARTITION BY phone_number ORDER BY created_at DESC) AS rn
            FROM customer_sale_record
            WHERE created_at BETWEEN :start AND :end
        )
        SELECT *
        FROM ranked
        WHERE rn = 1
    """, nativeQuery = true)
    List<CustomerSaleRecord> findDistinctCustomerByCreatedAt(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT
            DATE(created_at) AS created_date,
            COUNT(*) AS customer_count
        FROM
            customer_sale_record
        WHERE
            created_at BETWEEN :start AND :end
            AND (customer_type LIKE 'KH trải nghiệm' OR customer_type IS NULL)
        GROUP BY
            DATE(created_at)
        ORDER BY
            DATE(created_at);
    """, nativeQuery = true)
    List<DailyCustomerCount> countNewCustomersByDate(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT
            DATE(created_at) AS created_date,
            COUNT(*) AS customer_count
        FROM
            customer_sale_record
        WHERE
            created_at BETWEEN :start AND :end
            AND customer_type IS NOT NULL
            AND customer_type != 'KH trải nghiệm'
        GROUP BY
            DATE(created_at)
        ORDER BY
            DATE(created_at);
    """, nativeQuery = true)
    List<DailyCustomerCount> countOldCustomersByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    @Query(value = """
        SELECT
            gender,
            COUNT(*) AS total
        FROM
            customer_sale_record
        WHERE
            created_at BETWEEN :start AND :end
            AND (customer_type LIKE 'KH trải nghiệm' OR customer_type IS NULL)
        GROUP BY
            gender;
    """, nativeQuery = true)
    List<Object[]> countGenderGroup(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Query(value =
            "SELECT customer_type, DATE(created_at) as day, COUNT(*) as total " +
                    "FROM customer_sale_record " +
                    "WHERE created_at BETWEEN :start AND :end " +
                    "GROUP BY customer_type, day " +
                    "ORDER BY day", nativeQuery = true)
    List<Object[]> countCustomerByTypeAndDay(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query(value =
            "SELECT source, DATE(created_at) as day, COUNT(*) as total " +
                    "FROM customer_sale_record " +
                    "WHERE created_at BETWEEN :start AND :end " +
                    "GROUP BY source, day " +
                    "ORDER BY day", nativeQuery = true)
    List<Object[]> countCustomerBySourceAndDay(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Query(value = "SELECT DISTINCT phone_number FROM customer_sale_record " +
            "WHERE created_at BETWEEN :start AND :end", nativeQuery = true)
    List<String> findPhonesByCreatedAtBetween(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query(value = "SELECT DISTINCT phone_number FROM customer_sale_record WHERE created_at < :before", nativeQuery = true)
    List<String> findPhonesByCreatedAtBefore(@Param("before") LocalDateTime before);

}