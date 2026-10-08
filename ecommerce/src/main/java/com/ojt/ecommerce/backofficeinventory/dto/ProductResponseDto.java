package com.ojt.ecommerce.backofficeinventory.dto;

import java.time.LocalDateTime;

import com.ojt.ecommerce.enums.ProductStatus;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {

    private Long productId;
    private String productName;
    private String description;
    private ProductStatus status;

    private Long categoryId;
    private String categoryName;

    private Long brandId;
    private String brandName;

    private String imageUrl;
    private String sku;
    private java.math.BigDecimal sellingPrice;
    private Integer stock;

    private Long createdByUserId;
    private LocalDateTime createdAt;
    private Long modifiedByUserId;
    private LocalDateTime modifiedAt;
}