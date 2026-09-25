package com.ojt.ecommerce.storefront.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data public class CustomerAddressRequest {
    private String label;
    @NotBlank private String recipientName;
    @NotBlank private String phoneNumber;
    @NotBlank private String addressLine1;
    private String addressLine2;
    @NotBlank private String township;
    @NotBlank private String city;
    private String regionOrState;
    private String postalCode;
    @NotNull private Boolean isDefault;
}