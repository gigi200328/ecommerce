package com.ojt.ecommerce.backofficeinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to adjust stock to a specific physical count")
public class StockAdjustmentRequestDto {

    @NotNull(message = "Variant ID is required")
    @Schema(description = "Product Variant ID", example = "1")
    private Long variantId;

    @NotNull(message = "New quantity is required")
    @Min(value = 0, message = "Adjusted quantity cannot be negative")
    @Schema(description = "Actual physical quantity counted", example = "45")
    private Integer newQuantity;

    @NotBlank(message = "Adjustment remark/reason is required")
    @Schema(description = "Reason for discrepancy adjustment", example = "Quarterly physical stock audit")
    private String remark;
}
