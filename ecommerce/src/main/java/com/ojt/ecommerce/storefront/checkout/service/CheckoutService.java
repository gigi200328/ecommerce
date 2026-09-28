package com.ojt.ecommerce.storefront.checkout.service;
import com.ojt.ecommerce.storefront.checkout.dto.*;
public interface CheckoutService {
    OrderResponse createOrder(Long customerId, CreateOrderRequest request);
}