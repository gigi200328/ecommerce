package com.ojt.ecommerce.storefront.dto;
import lombok.Data;
@Data public class CustomerAddressResponse {
    private Long addressId;
    private String label;
    private String recipientName;
    private String phoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String township;
    private String city;
    private String regionOrState;
    private String postalCode;
    private Boolean isDefault;
}