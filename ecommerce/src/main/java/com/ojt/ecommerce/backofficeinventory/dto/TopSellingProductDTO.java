package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;

/**
 * Interface-based DTO Projection for mapping the Top Selling Products native query results.
 */
public interface TopSellingProductDTO {
    
    String getProductName();        // Maps to product_name
    
    String getCategoryName();       // Maps to category_name
    
    Long getTotalQuantitySold();    // Maps to total_quantity_sold
    
    BigDecimal getTotalRevenue();   // Maps to total_revenue
}
