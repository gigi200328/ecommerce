package com.ojt.ecommerce.backofficeinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to restock an existing product variant")
public class RestockRequestDto {

    @NotNull(message = "Variant ID is required")
    @Schema(description = "Product Variant ID", example = "1")
    private Long variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Restock quantity must be at least 1")
    @Schema(description = "Number of units to add to inventory", example = "50")
    private Integer quantity;

    @Schema(description = "Reference document type (e.g. MANUAL, PURCHASE_ORDER, SHIPMENT)", example = "PURCHASE_ORDER")
    private String referenceType;

    @Schema(description = "Reference ID (e.g. Purchase Order ID)", example = "1001")
    private Long referenceId;

    @Schema(description = "Optional notes or remark", example = "Restocked from Supplier Batch #892")
    private String remark;
}
