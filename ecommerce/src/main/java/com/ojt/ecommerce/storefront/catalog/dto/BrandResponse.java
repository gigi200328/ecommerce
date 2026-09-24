package com.ojt.ecommerce.storefront.catalog.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class BrandResponse {
    private Long brandId;
    private String brandName;
    private String brandLogoUrl;
}