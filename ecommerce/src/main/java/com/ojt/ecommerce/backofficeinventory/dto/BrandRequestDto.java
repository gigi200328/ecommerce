package com.ojt.ecommerce.backofficeinventory.dto;

import com.ojt.ecommerce.entity.BrandStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BrandRequestDto {

    @NotBlank(message = "Brand name is required")
    @Size(max = 100, message = "Brand name must not exceed 100 characters")
    private String brandName;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String brandLogoUrl;

    private String description;

    @NotNull(message = "Status is required")
    private BrandStatus status;
}