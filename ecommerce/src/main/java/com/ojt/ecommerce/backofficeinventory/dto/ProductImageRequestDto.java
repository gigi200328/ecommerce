package com.ojt.ecommerce.backofficeinventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRequestDto {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Image file is required")
    private MultipartFile file;

    private Boolean isPrimary;

    @NotNull(message = "User ID is required")
    private Long userId;
}