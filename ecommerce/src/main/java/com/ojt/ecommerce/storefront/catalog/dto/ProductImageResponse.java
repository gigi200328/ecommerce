package com.ojt.ecommerce.storefront.catalog.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class ProductImageResponse {
    private Long imageId;
    private String imageUrl;
    private Boolean isPrimary;
}