package com.ojt.ecommerce.backofficeinventory.dto;

import com.ojt.ecommerce.entity.ProductStatus;
import java.time.LocalDateTime;
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

    private Long createdByUserId;
    private LocalDateTime createdAt;
    private Long modifiedByUserId;
    private LocalDateTime modifiedAt;
}