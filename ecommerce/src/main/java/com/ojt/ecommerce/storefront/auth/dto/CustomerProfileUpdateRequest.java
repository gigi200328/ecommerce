package com.ojt.ecommerce.storefront.auth.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class CustomerProfileUpdateRequest {
    @NotBlank private String fullName;
    private String phone;
}