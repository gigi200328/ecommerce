package com.ojt.ecommerce.storefront.order.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class OrderAddressSnapshot {
    private String recipientName;
    private String phoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String township;
    private String city;
    private String regionOrState;
}