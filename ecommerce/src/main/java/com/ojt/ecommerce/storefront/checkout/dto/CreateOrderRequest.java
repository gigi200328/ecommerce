package com.ojt.ecommerce.storefront.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private String checkoutType;
    private BuyNowItemRequest buyNowItem;
    private Long savedAddressId;
    private CustomShippingAddressRequest shippingAddress;
}
