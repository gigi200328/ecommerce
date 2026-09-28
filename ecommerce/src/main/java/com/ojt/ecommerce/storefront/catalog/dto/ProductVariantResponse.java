package com.ojt.ecommerce.storefront.catalog.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data @Builder public class ProductVariantResponse {
    private Long variantId;
    private String sku;
    private BigDecimal sellingPrice;
    private BigDecimal discountPrice;
    private String status;
    private Integer availableQuantity;
    private String stockStatus;
    private List<VariantOptionResponse> options;
}