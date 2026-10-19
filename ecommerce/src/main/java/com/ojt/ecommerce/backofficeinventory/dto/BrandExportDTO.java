package com.ojt.ecommerce.backofficeinventory.dto;

/**
 * Interface-based DTO Projection for mapping SP_GetBrandExportReport results.
 * Spring Data JPA automatically maps the result set columns:
 * - brand_name / brandName       -> getBrandName()
 * - category_name / categoryName -> getCategoryName()
 * - description                  -> getDescription()
 * - status                       -> getStatus()
 */
public interface BrandExportDTO {

    String getBrandName();

    String getCategoryName();

    String getDescription();

    String getStatus();
}
