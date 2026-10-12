package com.ojt.ecommerce.storefront.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private Long customerId;
    private String fullName;
    private String email;
    private String phone;
    private String profileImageUrl;
}