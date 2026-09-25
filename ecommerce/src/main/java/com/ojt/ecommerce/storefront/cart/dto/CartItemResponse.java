package com.ojt.ecommerce.storefront.cart.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;
@Data @Builder public class CartItemResponse {
    private Long cartItemId;
    private Long variantId;
    private Long productId;
    private String productName;
    private String imageUrl;
    private String sku;
    private Map<String, String> options;
    private Integer quantity;
    private BigDecimal effectivePrice;
    private BigDecimal subtotal;
    private Integer availableQuantity;
    private String stockStatus;
    private Boolean available;
}