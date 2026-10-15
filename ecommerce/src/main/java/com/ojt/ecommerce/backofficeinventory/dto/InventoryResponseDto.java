package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Inventory details for a product variant")
public class InventoryResponseDto {

    @Schema(description = "Inventory ID", example = "1")
    private Long inventoryId;

    @Schema(description = "Variant ID", example = "1")
    private Long variantId;

    @Schema(description = "SKU Code", example = "NIKE-AIR-BLK-42")
    private String sku;

    @Schema(description = "Associated Product Name", example = "Nike Air Max")
    private String productName;

    @Schema(description = "Selling Price", example = "149.99")
    private BigDecimal sellingPrice;

    @Schema(description = "Total physical stock quantity", example = "100")
    private Integer quantity;

    @Schema(description = "Reserved stock quantity for pending orders", example = "5")
    private Integer reservedQuantity;

    @Schema(description = "Available stock for new purchases (quantity - reservedQuantity)", example = "95")
    private Integer availableQuantity;

    @Schema(description = "Minimum threshold to trigger restock alert", example = "10")
    private Integer reorderLevel;

    @Schema(description = "Stock status: NORMAL, LOW_STOCK, OUT_OF_STOCK", example = "NORMAL")
    private String status;

    @Schema(description = "Whether the item is perishable/expirable", example = "true")
    private Boolean isPerishable;

    @Schema(description = "Expiration date of the batch", example = "2026-12-31")
    private LocalDate expiryDate;

    @Schema(description = "Batch or lot number", example = "LOT-2026-001")
    private String batchNumber;

    @Schema(description = "Days remaining until expiration", example = "45")
    private Long daysUntilExpiry;

    @Schema(description = "Expiration Status: GOOD, EXPIRING_SOON, EXPIRED, NON_PERISHABLE", example = "EXPIRING_SOON")
    private String expiryStatus;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Modified timestamp")
    private LocalDateTime modifiedAt;
}
