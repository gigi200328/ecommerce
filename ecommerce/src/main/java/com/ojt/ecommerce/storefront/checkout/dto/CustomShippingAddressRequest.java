package com.ojt.ecommerce.storefront.checkout.dto;
import lombok.Data;
@Data public class CustomShippingAddressRequest {
    private String recipientName;
    private String phoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String township;
    private String city;
    private String regionOrState;
}