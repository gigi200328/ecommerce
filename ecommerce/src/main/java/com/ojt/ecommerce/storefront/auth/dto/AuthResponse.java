package com.ojt.ecommerce.storefront.auth.dto;
import lombok.Data;
import lombok.Builder;
@Data @Builder public class AuthResponse {
    private String token;
    private Long customerId;
    private String fullName;
    private String email;
}