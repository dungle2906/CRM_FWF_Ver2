package com.example.BasicCRM_FWF.Repository;

import com.example.BasicCRM_FWF.Model.BookingRecord;
import com.example.BasicCRM_FWF.Model.BookingSalesMap;
import com.example.BasicCRM_FWF.Model.SalesTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingSalesMapRepository extends JpaRepository<BookingSalesMap, Long> {
    boolean existsByBookingAndSalesTransaction(BookingRecord booking, SalesTransaction salesTransaction);
}