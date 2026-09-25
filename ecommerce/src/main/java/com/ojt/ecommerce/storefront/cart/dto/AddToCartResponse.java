package com.ojt.ecommerce.storefront.cart.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class AddToCartResponse {
    private Long cartItemId;
    private Long cartId;
    private Long variantId;
    private Integer quantity;
}