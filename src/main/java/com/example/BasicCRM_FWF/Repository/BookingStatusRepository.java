package com.example.BasicCRM_FWF.Repository;

import com.example.BasicCRM_FWF.Model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStatusRepository extends JpaRepository<BookingStatus, Integer> {
}
