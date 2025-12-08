package com.example.BasicCRM_FWF.Repository;

import com.example.BasicCRM_FWF.Model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Integer> {
//    Shift findByEmployeeNameAndDate(String employeeName, LocalDate date);

    @Query(value = """
        SELECT * FROM Shift WHERE employee_name = :employeeName AND date = :date
    """,nativeQuery = true)
    Shift findByNameAndDate(String employeeName, LocalDate date);

    // Sử dụng CONCAT để thêm '%' vào giá trị truyền vào
    @Query(value = "SELECT s FROM Shift s WHERE s.date LIKE CONCAT(:date, '%')")
    List<Shift> findAllShiftsByDate(@Param("date") LocalDate date);
}
