package com.ojt.ecommerce.storefront.catalog.dto;
import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data @Builder public class CategoryNodeResponse {
    private Long categoryId;
    private String categoryName;
    private List<CategoryNodeResponse> children;
}