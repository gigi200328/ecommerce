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
@Schema(description = "Request payload to write off damaged, expired, or lost stock")
public class WriteOffRequestDto {

    @NotNull(message = "Variant ID is required")
    @Schema(description = "Product Variant ID", example = "1")
    private Long variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Write-off quantity must be at least 1")
    @Schema(description = "Number of units to remove from inventory", example = "3")
    private Integer quantity;

    @NotBlank(message = "Reason / remark is required for write-off")
    @Schema(description = "Reason for writing off stock (e.g. DAMAGED_GOODS, EXPIRED, LOST_IN_TRANSIT)", example = "Damaged during warehouse transit")
    private String remark;
}
