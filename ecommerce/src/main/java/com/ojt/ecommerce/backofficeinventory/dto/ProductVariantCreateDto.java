package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import java.util.List;

import com.ojt.ecommerce.entity.ProductVariantStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload to create a Product Variant together with variation options and initial stock")
public class ProductVariantCreateDto {

    @Schema(description = "Associated Product ID", example = "1")
    private Long productId;

    @Schema(description = "Stock Keeping Unit (SKU), auto-generated if left empty", example = "NK-BLK-42")
    private String sku;

    @Schema(description = "Selling price", example = "149.99")
    private BigDecimal sellingPrice;

    @Schema(description = "Cost price", example = "90.00")
    private BigDecimal costPrice;

    @Schema(description = "Variant status", example = "ACTIVE")
    private ProductVariantStatus status;

    @Schema(description = "List of Variation Option IDs to map to this variant", example = "[1, 5]")
    private List<Long> optionIds;

    @Schema(description = "Initial stock quantity to place in inventory", example = "20")
    private Integer initialStock;
}
