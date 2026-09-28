package com.ojt.ecommerce.storefront.catalog.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class VariantOptionResponse {
    private Long variationId;
    private String variationName;
    private Long optionId;
    private String value;
}