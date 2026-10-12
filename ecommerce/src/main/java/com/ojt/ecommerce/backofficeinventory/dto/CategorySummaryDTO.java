package com.ojt.ecommerce.backofficeinventory.dto;

/**
 * Interface-based DTO Projection for mapping the Category Summary native query results.
 */
public interface CategorySummaryDTO {
    
    String getCategoryName();   // Maps to category_name
    
    String getParentCategory(); // Maps to parent_category
    
    String getDescription();    // Maps to description
    
    Long getTotalProducts();    // Maps to total_products
}
