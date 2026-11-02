package com.ojt.ecommerce.storefront.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyNowItemRequest {
    private Long variantId;
    private Integer quantity;
}
