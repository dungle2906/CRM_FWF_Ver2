package com.example.BasicCRM_FWF.Repository;

import com.example.BasicCRM_FWF.Model.SalesTransaction;
import com.example.BasicCRM_FWF.Model.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {

    @Query("SELECT s.phoneNumber FROM ServiceRecord s WHERE s.bookingDate BETWEEN :from AND :to")
    List<String> findPhonesBetweenBookingDate(LocalDateTime from, LocalDateTime to);

    @Query("SELECT s.phoneNumber FROM ServiceRecord s")
    List<String> findPhones();

    @Query(value = "SELECT DISTINCT phone_number FROM service_record " +
            "WHERE booking_date BETWEEN :start AND :end " +
            "AND base_service_id IS NOT NULL AND base_service_id <> ''", nativeQuery = true)
    List<String> findServicePhonesWithBaseServiceBetween(@Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);

    List<ServiceRecord> findByBookingDateBetween(LocalDateTime start, LocalDateTime end);

    @Query(value = "SELECT DATE(booking_date) AS date, " +
            "CASE " +
            " WHEN st.service_name LIKE 'COMBO CS%' THEN 'Combo CS' " +
            " WHEN st.service_name LIKE 'COMBO%' THEN 'Combo' " +
            " WHEN st.service_name LIKE 'DV%' THEN 'Dịch vụ' " +
            " WHEN st.service_name LIKE 'CT%' THEN 'Cộng thêm' " +
            " WHEN st.service_name LIKE 'QUÀ TẶNG%' THEN 'Quà tặng' " +
            " ELSE 'Khác' END AS type, " +
            "COUNT(*) AS total " +
            "FROM service_record sr " +
            "JOIN service_type_temp st ON sr.base_service_id = st.id " +
            "WHERE booking_date BETWEEN :start AND :end " +
            "GROUP BY DATE(booking_date), type " +
            "ORDER BY DATE(booking_date), type", nativeQuery = true)
    List<Object[]> countServiceTypesPerDay(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    @Query(value = "SELECT COUNT(*) FROM service_record sr \n" +
            "JOIN service_type_temp st ON sr.base_service_id = st.id \n" +
            "WHERE sr.booking_date BETWEEN :start AND :end \n" +
            "AND st.service_name LIKE :prefix%", nativeQuery = true)
    long countByServiceCodePrefix(@Param("prefix") String prefix,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT\s
            r.region AS shop,
            CASE\s
                WHEN st.service_name LIKE 'COMBO CS%' THEN 'Combo CS'
                WHEN st.service_name LIKE 'COMBO%' THEN 'Combo'
                WHEN st.service_name LIKE 'DV%' THEN 'Dịch vụ'
                WHEN st.service_name LIKE 'CT%' THEN 'Cộng thêm'
                WHEN st.service_name LIKE 'QUÀ TẶNG%' THEN 'Quà tặng'
                ELSE 'Khác'
            END AS type,
            COUNT(*) AS total
        FROM service_record sr
        JOIN service_type_temp st\s
            ON sr.base_service_id = st.id
        JOIN region r\s
            ON sr.facility_id = r.id
        WHERE sr.booking_date BETWEEN :start AND :end
        GROUP BY r.region, type
        ORDER BY r.region, type;
    """, nativeQuery = true)
    List<Object[]> findRegionServiceTypeCount(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT\s
                r.shop_name AS shop,
                CASE\s
                    WHEN st.service_name LIKE 'COMBO CS%' THEN 'Combo CS'
                    WHEN st.service_name LIKE 'COMBO%' THEN 'Combo'
                    WHEN st.service_name LIKE 'DV%' THEN 'Dịch vụ'
                    WHEN st.service_name LIKE 'CT%' THEN 'Cộng thêm'
                    WHEN st.service_name LIKE 'QUÀ TẶNG%' THEN 'Quà tặng'
                    ELSE 'Khác'
                END AS type,
                COUNT(*) AS total
            FROM service_record sr
            JOIN service_type_temp st\s
                ON sr.base_service_id = st.id
            JOIN region r\s
                ON sr.facility_id = r.id
            WHERE sr.booking_date BETWEEN :start AND :end
            GROUP BY r.shop_name, type
            ORDER BY r.shop_name, type;
    """, nativeQuery = true)
    List<Object[]> findServiceUsageByShop(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    // Query top 10 service names by count within date range
    @Query(value = "SELECT st.service_name, COUNT(*) AS cnt " +
            "FROM service_record sr " +
            "JOIN service_type_temp st ON sr.base_service_id = st.id " +
            "WHERE sr.booking_date BETWEEN :start AND :end " +
            "GROUP BY st.service_name " +
            "ORDER BY cnt DESC " +
            "LIMIT 10", nativeQuery = true)
    List<Object[]> findTop10ServiceNames(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query(value = "SELECT st.service_name, SUM(sr.session_price) AS totalRevenue " +
            "FROM service_record sr " +
            "JOIN service_type_temp st ON sr.base_service_id = st.id " +
            "WHERE sr.booking_date BETWEEN :start AND :end " +
            "GROUP BY st.service_name " +
            "ORDER BY totalRevenue DESC " +
            "LIMIT 10", nativeQuery = true)
    List<Object[]> findTop10ServicesByRevenue(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT st.service_name, SUM(sr.session_price) AS totalRevenue
        FROM service_record sr
        JOIN service_type_temp st ON sr.base_service_id = st.id
        WHERE sr.booking_date BETWEEN :start AND :end
        GROUP BY st.service_name
        HAVING totalRevenue > 10000
        ORDER BY totalRevenue ASC LIMIT 5
        """, nativeQuery = true
    )
    List<Object[]> findTopBottomServicesRevenue(@Param("start" ) LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT st.service_name, COUNT(*) AS cnt
        FROM service_record sr
        JOIN service_type_temp st ON sr.base_service_id = st.id
        WHERE sr.booking_date BETWEEN :start AND :end
        GROUP BY st.service_name
        ORDER BY cnt ASC
        LIMIT 5
    """, nativeQuery = true)
    List<Object[]> findTopBottomServicesUsage(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Repository
    @Query(value = """
        SELECT 
            st.service_name,
            CASE 
                WHEN st.service_name LIKE 'COMBO CS%' THEN 'Combo CS'
                WHEN st.service_name LIKE 'COMBO%' THEN 'Combo'
                WHEN st.service_name LIKE 'DV%' THEN 'Dịch vụ'
                WHEN st.service_name LIKE 'CT%' THEN 'Cộng thêm'
                WHEN st.service_name LIKE 'QUÀ TẶNG%' THEN 'Quà tặng'
                ELSE 'Khác' END AS type,
            COUNT(*) AS currentCount,
            SUM(sr.session_price) AS currentRevenue
        FROM service_record sr
        JOIN service_type_temp st ON sr.base_service_id = st.id
        WHERE sr.booking_date BETWEEN :start AND :end
        GROUP BY st.service_name, type
        ORDER BY currentCount DESC
    """, nativeQuery = true)
    List<Object[]> findTop10ServicesWithCurrentData(LocalDateTime start, LocalDateTime end);

    @Query(value = """
        SELECT 
            st.service_name,
            COUNT(*) AS previousCount,
            SUM(sr.session_price) AS previousRevenue
        FROM service_record sr
        JOIN service_type_temp st ON sr.base_service_id = st.id
        WHERE sr.booking_date BETWEEN :prevStart AND :prevEnd
        GROUP BY st.service_name
    """, nativeQuery = true)
    List<Object[]> findTop10ServicesWithPreviousData(LocalDateTime prevStart, LocalDateTime prevEnd);

    @Query(value = """
       SELECT COUNT(*) AS total_non_ct
       FROM temp_crm.service_record sr
       LEFT JOIN temp_crm.service_type_temp stt
              ON sr.base_service_id = stt.id
       WHERE COALESCE(stt.service_name, sr.service_name) NOT LIKE 'CT%'
         AND sr.booking_date >= :start
         AND sr.booking_date <=  :end
    """, nativeQuery = true)
    long findByServiceOrderDateBetween(LocalDateTime start, LocalDateTime end);

    @Query(value = """
    SELECT COUNT(*)
            FROM service_record sr
            WHERE sr.booking_date >= :start
              AND sr.booking_date <= :end
              AND sr.phone_number IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM customer_sale_record c
                  WHERE c.phone_number = sr.phone_number
                    AND c.gender IS NOT NULL
                    AND LOWER(c.gender) = LOWER(:gender)
              )
    """, nativeQuery = true)
    long countServiceByGenderBetween(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     @Param("gender") String gender);
}