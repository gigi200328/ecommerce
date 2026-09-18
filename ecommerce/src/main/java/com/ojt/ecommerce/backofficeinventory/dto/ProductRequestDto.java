package com.ojt.ecommerce.backofficeinventory.dto;

import com.ojt.ecommerce.entity.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductRequestDto {

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String productName;

    private String description;

    @NotNull(message = "Status is required")
    private ProductStatus status;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private Long brandId;

    @NotNull(message = "User ID is required")
    private Long userId;
}