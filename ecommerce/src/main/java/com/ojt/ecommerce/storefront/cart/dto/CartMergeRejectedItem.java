package com.ojt.ecommerce.storefront.cart.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class CartMergeRejectedItem {
    private Long variantId;
    private String reason;
}