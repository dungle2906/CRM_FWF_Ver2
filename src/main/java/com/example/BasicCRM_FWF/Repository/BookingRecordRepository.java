package com.example.BasicCRM_FWF.Repository;

import com.example.BasicCRM_FWF.Model.BookingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRecordRepository extends JpaRepository<BookingRecord, Integer> {

    @Query("SELECT b.phone_number FROM BookingRecord b WHERE b.created_date BETWEEN :from AND :to")
    List<String> findPhonesBetweenCreatedDate(LocalDateTime from, LocalDateTime to);

    @Query("SELECT b.phone_number FROM BookingRecord b")
    List<String> findPhones();

    @Query("""
        SELECT br
        FROM BookingRecord br
        JOIN br.bookingStatus bs
        WHERE br.booking_date BETWEEN :start AND :end
          AND LOWER(bs.status) = :arrivedStatus
          AND br.facility IS NOT NULL
        """)
    List<BookingRecord> findArrivalsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("arrivedStatus") String arrivedStatus);

    @Query(value = """
        SELECT bs.status AS status, COUNT(br.customer_amount) AS count
        FROM booking_record br
        JOIN booking_status bs ON bs.id = br.booking_status_id
        WHERE br.booking_date BETWEEN :start AND :end
        GROUP BY bs.status
        ORDER BY count DESC
    """, nativeQuery = true)
    List<Object[]> countBookingByStatusBetween(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Query("""
        SELECT br.customerStatus, COUNT(br.customer_amount)
        FROM BookingRecord br
        WHERE br.booking_date BETWEEN :start AND :end
          AND br.phone_number IS NOT NULL
        GROUP BY br.customerStatus
""")
    List<Object[]> countByCustomerStatus(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query("""
        SELECT br.phone_number, MAX(br.customer_name), COUNT(br.customer_amount) as cnt
        FROM BookingRecord br
        WHERE br.booking_date BETWEEN :start AND :end
          AND br.phone_number IS NOT NULL
        GROUP BY br.phone_number
        ORDER BY cnt DESC LIMIT 10
    """)
    List<Object[]> findTopCustomers(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    //    @Query(value = "SELECT br.booking_employee AS employee, COUNT(*) AS total " +
//            "FROM booking_record br " +
//               "WHERE br.booking_date BETWEEN :start AND :end " +
//               "GROUP BY br.booking_employee " +
//                       "ORDER BY total DESC " +
//                       "LIMIT 10",
//    nativeQuery = true)
    @Query(value = """
        SELECT
            br.booking_employee AS employee,
            SUM(COALESCE(br.customer_amount, 0)) AS total
        FROM booking_record br
        WHERE br.booking_date BETWEEN :start AND :end
          AND br.booking_status_id = 3
        GROUP BY br.booking_employee
        ORDER BY total DESC
        LIMIT 10
    """, nativeQuery = true)
    List<Object[]> findTopBookingEmployees(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    // Tổng lượt khách mới
    @Query(value = "SELECT SUM(customer_amount) " +
            "FROM temp_crm.booking_record " +
            "WHERE customer_status = :cusStatus " +
            "AND booking_date BETWEEN :fromDate AND :toDate",
            nativeQuery = true)
    long countByCustomerStatusAndBookingDateBetween(@Param("cusStatus") int cusStatus,
                                                    @Param("fromDate") LocalDateTime fromDate,
                                                    @Param("toDate") LocalDateTime toDate);

    // Tổng lượt khách mới thực đi
    @Query(value = "SELECT SUM(customer_amount) " +
            "FROM temp_crm.booking_record " +
            "WHERE customer_status = :cusStatus " +
            "AND booking_status_id = 3 " +
            "AND booking_date BETWEEN :fromDate AND :toDate",
            nativeQuery = true)
    long countByCustomerStatusAndBookingStatusIdAndBookingDateBetween(@Param("cusStatus") int cusStatus,
                                                                      @Param("fromDate") LocalDateTime fromDate,
                                                                      @Param("toDate") LocalDateTime toDate);
}
