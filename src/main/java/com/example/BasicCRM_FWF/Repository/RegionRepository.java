package com.example.BasicCRM_FWF.Repository;

import com.example.BasicCRM_FWF.Model.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RegionRepository extends JpaRepository<Region, Long> {

    @Query(value = """
        SELECT * FROM region WHERE stock_id LIKE :stockId;
        """, nativeQuery = true)
    Region findAllByStock_id(String stockId);

}
