package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Interface-based DTO Projection for mapping native query results.
 * Spring Data JPA will automatically map the result set columns to these getters.
 */
public interface DailyProductSalesDTO {
    
    Date getSalesDate();      // Maps to sales_date
    
    String getProductName();  // Maps to product_name
    
    String getCategoryName(); // Maps to category_name
    
    Long getQuantitySold();   // Maps to quantity_sold
    
    BigDecimal getPricePerUnit(); // Maps to price_per_unit
    
    BigDecimal getTotalSales();   // Maps to total_sales
}
