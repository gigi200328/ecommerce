package com.ojt.ecommerce.storefront.catalog.dto;
import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data @Builder public class ProductDetailResponse {
    private Long productId;
    private String productName;
    private String description;
    private Long categoryId;
    private String categoryName;
    private BrandResponse brand;
    private String status;
    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;
    private List<String> tags;
}
