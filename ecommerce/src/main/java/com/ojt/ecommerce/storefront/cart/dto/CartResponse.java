package com.ojt.ecommerce.storefront.cart.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data @Builder public class CartResponse {
    private Long cartId;
    private String status;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private List<CartItemResponse> items;
}