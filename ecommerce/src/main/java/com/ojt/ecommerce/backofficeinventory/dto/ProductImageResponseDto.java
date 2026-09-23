package com.ojt.ecommerce.backofficeinventory.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponseDto {

    private Long imageId;
    private Long productId;
    private String imageUrl;
    private Boolean isPrimary;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private Long modifiedByUserId;
    private LocalDateTime modifiedAt;
}