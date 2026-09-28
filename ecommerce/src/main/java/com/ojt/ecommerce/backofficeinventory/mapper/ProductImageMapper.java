package com.ojt.ecommerce.backofficeinventory.mapper;

import com.ojt.ecommerce.backofficeinventory.dto.ProductImageResponseDto;
import com.ojt.ecommerce.entity.ProductImage;
import org.springframework.stereotype.Component;

@Component
public class ProductImageMapper {

    public ProductImageResponseDto toResponseDto(ProductImage image) {
        if (image == null) {
            return null;
        }

        return ProductImageResponseDto.builder()
                .imageId(image.getImageId())
                .productId(image.getProduct() != null ? image.getProduct().getProductId() : null)
                .imageUrl(image.getImageUrl())
                .isPrimary(image.getIsPrimary())
                .createdByUserId(image.getCreatedBy() != null ? (long) image.getCreatedBy().getUserId() : null)
                .createdAt(image.getCreatedAt())
                .modifiedByUserId(image.getModifiedBy() != null ? (long) image.getModifiedBy().getUserId() : null)
                .modifiedAt(image.getModifiedAt())
                .build();
    }
}