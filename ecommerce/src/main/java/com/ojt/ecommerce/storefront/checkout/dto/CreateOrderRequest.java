package com.ojt.ecommerce.storefront.checkout.dto;
import lombok.Data;
@Data public class CreateOrderRequest {
    private Long savedAddressId;
    private CustomShippingAddressRequest shippingAddress;
}