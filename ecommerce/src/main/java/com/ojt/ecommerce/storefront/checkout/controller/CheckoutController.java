package com.ojt.ecommerce.storefront.checkout.controller;


import com.ojt.ecommerce.storefront.checkout.dto.CreateOrderRequest;
import com.ojt.ecommerce.storefront.checkout.dto.OrderResponse;
import com.ojt.ecommerce.storefront.checkout.service.CheckoutService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storefront/v1/orders")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(checkoutService.createOrder(principal.getCustomerId(), request));
    }
}
