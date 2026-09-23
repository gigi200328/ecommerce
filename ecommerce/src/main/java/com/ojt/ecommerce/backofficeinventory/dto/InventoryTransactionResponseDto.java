package com.ojt.ecommerce.backofficeinventory.dto;

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
@Schema(description = "Inventory transaction audit trail record")
public class InventoryTransactionResponseDto {

    @Schema(description = "Transaction ID", example = "1")
    private Long txnId;

    @Schema(description = "Variant ID", example = "1")
    private Long variantId;

    @Schema(description = "SKU Code", example = "NIKE-AIR-BLK-42")
    private String sku;

    @Schema(description = "Product Name", example = "Nike Air Max")
    private String productName;

    @Schema(description = "Transaction Type: RESTOCK, SALE, RETURN, WRITE_OFF, ADJUSTMENT", example = "RESTOCK")
    private String txnType;

    @Schema(description = "Quantity change (positive for in, negative for out)", example = "50")
    private Integer qty;

    @Schema(description = "Stock quantity before transaction", example = "0")
    private Integer beforeQty;

    @Schema(description = "Stock quantity after transaction", example = "50")
    private Integer afterQty;

    @Schema(description = "Reference document type (e.g. PURCHASE_ORDER, MANUAL)", example = "PURCHASE_ORDER")
    private String referenceType;

    @Schema(description = "Reference ID", example = "1001")
    private Long referenceId;

    @Schema(description = "Remarks / Notes", example = "Supplier shipment received")
    private String remark;

    @Schema(description = "Username who initiated transaction", example = "System Admin")
    private String createdByName;

    @Schema(description = "Transaction timestamp")
    private LocalDateTime createdAt;
}
