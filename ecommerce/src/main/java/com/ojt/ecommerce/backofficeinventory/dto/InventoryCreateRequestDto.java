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
@Schema(description = "Request to initialize inventory tracking for a variant")
public class InventoryCreateRequestDto {

    @NotNull(message = "Variant ID is required")
    @Schema(description = "Product Variant ID", example = "1")
    private Long variantId;

    @Min(value = 0, message = "Initial quantity cannot be negative")
    @Schema(description = "Initial physical stock quantity", example = "50", defaultValue = "0")
    @Builder.Default
    private Integer initialQuantity = 0;

    @Min(value = 0, message = "Reorder level cannot be negative")
    @Schema(description = "Reorder threshold level", example = "10", defaultValue = "10")
    @Builder.Default
    private Integer reorderLevel = 10;
}
