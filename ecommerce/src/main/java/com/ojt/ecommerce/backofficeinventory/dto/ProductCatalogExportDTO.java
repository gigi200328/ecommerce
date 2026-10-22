package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;

/**
 * Interface-based DTO Projection for mapping SP_GetProductCatalogExport results.
 * Spring Data JPA automatically maps the columns:
 * - product_name -> getProductName()
 * - sku          -> getSku()
 * - price        -> getPrice()
 * - stock        -> getStock()
 * - status       -> getStatus()
 */
public interface ProductCatalogExportDTO {

    String getProductName();

    String getSku();

    BigDecimal getPrice();

    Integer getStock();

    String getStatus();
}
