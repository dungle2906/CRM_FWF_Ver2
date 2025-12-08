package com.example.BasicCRM_FWF.Repository;

import com.example.BasicCRM_FWF.Model.AppUsageRecord;
import com.example.BasicCRM_FWF.Model.CustomerSaleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppUsageRecordRepository extends JpaRepository<AppUsageRecord, Long> {

    @Query(value = """
        WITH ranked AS (
            SELECT *,
                   ROW_NUMBER() OVER (PARTITION BY phone_number ORDER BY installed_at DESC) AS rn
            FROM app_usage_record
            WHERE installed_at BETWEEN :start AND :end
        )
        SELECT *
        FROM ranked
        WHERE rn = 1;
    """, nativeQuery = true)
    List<AppUsageRecord> findDistinctCustomerByInstalled(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT a.phoneNumber FROM AppUsageRecord a WHERE a.installedAt BETWEEN :from AND :to")
    List<String> findPhonesBetweenInstalledAt(LocalDateTime from, LocalDateTime to);

    @Query("SELECT a.phoneNumber FROM AppUsageRecord a")
    List<String> findPhones();

}
