package com.ojt.ecommerce.backofficeinventory.mapper;

import com.ojt.ecommerce.backofficeinventory.dto.BrandResponseDto;
import com.ojt.ecommerce.entity.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public BrandResponseDto toResponseDto(Brand brand) {
        if (brand == null) {
            return null;
        }

        return BrandResponseDto.builder()
                .brandId(brand.getBrandId())
                .brandName(brand.getBrandName())
                .brandLogoUrl(brand.getBrandLogoUrl())
                .description(brand.getDescription())
                .status(brand.getStatus())
                .createdAt(brand.getCreatedAt())
                .modifiedAt(brand.getModifiedAt())
                .build();
    }
}