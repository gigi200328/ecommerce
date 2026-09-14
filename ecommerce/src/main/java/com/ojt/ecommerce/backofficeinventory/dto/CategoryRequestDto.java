package com.ojt.ecommerce.backofficeinventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequestDto {

    @NotBlank(message = "Category name is required")
    @Size(max = 120, message = "Category name must not exceed 120 characters")
    private String categoryName;

    private String description;

    private Long parentId;

    @NotNull(message = "User ID is required")
    private Long userId;
}