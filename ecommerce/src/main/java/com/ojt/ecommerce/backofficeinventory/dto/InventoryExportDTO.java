package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;

/**
 * Interface-based DTO Projection for mapping SP_GetInventoryExportReport results.
 * Spring Data JPA automatically maps the result set columns:
 * - variant_and_sku / variantAndSku -> getVariantAndSku()
 * - product                        -> getProduct()
 * - selling_price / sellingPrice   -> getSellingPrice()
 * - physical_stock / physicalStock -> getPhysicalStock()
 * - reserved_stock / reservedStock -> getReservedStock()
 * - available_stock / availableStock -> getAvailableStock()
 * - reorder_level / reorderLevel   -> getReorderLevel()
 * - expiry_and_batch / expiryAndBatch -> getExpiryAndBatch()
 * - status                         -> getStatus()
 */
public interface InventoryExportDTO {

    String getVariantAndSku();

    String getProduct();

    BigDecimal getSellingPrice();

    Integer getPhysicalStock();

    Integer getReservedStock();

    Integer getAvailableStock();

    Integer getReorderLevel();

    String getExpiryAndBatch();

    String getStatus();
}
