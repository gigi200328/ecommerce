package com.ojt.ecommerce.backofficeinventory.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.backofficeinventory.dto.DailyProductSalesDTO;

@Repository
public interface ReportRepository extends JpaRepository<Product, Long> {

    // Calls the Stored Procedure and maps the result to the DailyProductSalesDTO interface
    @Query(value = "CALL SP_GetDailyProductSales(:startDate, :endDate)", nativeQuery = true)
    List<DailyProductSalesDTO> getDailyProductSales(
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate
    );
}
