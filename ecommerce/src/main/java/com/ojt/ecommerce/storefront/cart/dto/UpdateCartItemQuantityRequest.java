package com.ojt.ecommerce.storefront.cart.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data public class UpdateCartItemQuantityRequest {
    @NotNull private Integer quantity;
}