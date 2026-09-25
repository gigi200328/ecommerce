package com.ojt.ecommerce.storefront.cart.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data @Builder public class GuestCartResolveResponse {
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private List<GuestCartResolvedItemResponse> items;
}