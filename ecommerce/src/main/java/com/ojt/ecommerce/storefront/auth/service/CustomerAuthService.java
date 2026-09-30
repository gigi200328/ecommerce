package com.ojt.ecommerce.storefront.auth.service;
import com.ojt.ecommerce.storefront.auth.dto.*;
public interface CustomerAuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    CustomerMeResponse getCurrentCustomer(Long customerId);
    CustomerMeResponse updateCustomerProfile(Long customerId, CustomerProfileUpdateRequest request);
}