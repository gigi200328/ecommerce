package com.ojt.ecommerce.backofficeinventory.mapper;

import com.ojt.ecommerce.backofficeinventory.dto.ProductResponseDto;
import com.ojt.ecommerce.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponseDto toResponseDto(Product product) {
        if (product == null) {
            return null;
        }

        return ProductResponseDto.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .status(product.getStatus())
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getBrandId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : null)
                .createdByUserId(product.getCreatedBy() != null ? product.getCreatedBy().getUserId() : null)
                .createdAt(product.getCreatedAt())
                .modifiedByUserId(product.getModifiedBy() != null ? product.getModifiedBy().getUserId() : null)
                .modifiedAt(product.getModifiedAt())
                .build();
    }
}