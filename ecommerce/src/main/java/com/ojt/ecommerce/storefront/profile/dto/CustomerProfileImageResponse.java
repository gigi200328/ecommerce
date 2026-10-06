package com.ojt.ecommerce.storefront.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileImageResponse {
    private String profileImageUrl;
    private String message;
}
