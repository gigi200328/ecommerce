package com.ojt.ecommerce.storefront.catalog.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder public class ProductListResponse {
    private Long productId;
    private String productName;
    private String categoryName;
    private String brandName;
    private String primaryImageUrl;
    private BigDecimal startingPrice;
    private String status;
    private String stockStatus;
    private List<String> tags;
}
