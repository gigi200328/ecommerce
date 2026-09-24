package com.ojt.ecommerce.storefront.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomerPrincipal {
    private Long customerId;
    private String email;
}
