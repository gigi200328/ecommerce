package com.ojt.ecommerce.backofficeinventory.dto;

/**
 * Interface-based DTO Projection for mapping the Low Stock Alert native query results.
 */
public interface LowStockAlertDTO {
    
    String getSku();                // Maps to sku
    
    String getProductName();        // Maps to product_name
    
    String getCategoryName();       // Maps to category_name
    
    Integer getCurrentStock();      // Maps to current_stock
    
    Integer getReservedQuantity();  // Maps to reserved_quantity
    
    Integer getReorderLevel();      // Maps to reorder_level
    
    String getStockStatus();        // Maps to stock_status
}
