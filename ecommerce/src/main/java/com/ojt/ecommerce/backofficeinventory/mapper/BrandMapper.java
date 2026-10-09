
package com.ojt.ecommerce.backofficeinventory.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ojt.ecommerce.backofficeinventory.dto.BrandResponseDto;
import com.ojt.ecommerce.entity.Brand;

@Component
public class BrandMapper {

    public BrandResponseDto toResponseDto(Brand brand) {

        if (brand == null) {
            return null;
        }

        List<Long> categoryIds = brand.getBrandCategories()
                .stream()
                .map(brandCategory -> brandCategory.getCategory())
                .filter(category -> category != null)
                .map(category -> category.getCategoryId())
                .toList();

        return BrandResponseDto.builder()
                .brandId(brand.getBrandId())
                .brandName(brand.getBrandName())
                .brandLogoUrl(brand.getBrandLogoUrl())
                .description(brand.getDescription())
                .status(brand.getStatus())
                .categoryIds(categoryIds)
                .createdAt(brand.getCreatedAt())
                .modifiedAt(brand.getModifiedAt())
                .build();
    }
}

