package com.ojt.ecommerce.storefront.cart.controller;

import com.ojt.ecommerce.storefront.cart.dto.RestoreToCartResponse;
import com.ojt.ecommerce.storefront.cart.service.CartService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storefront/v1/orders")
@RequiredArgsConstructor
public class CartRecoveryController {

    private final CartService cartService;

    @PostMapping("/{orderId}/restore-to-cart")
    public ResponseEntity<RestoreToCartResponse> restoreToCart(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(cartService.restoreFailedCartOrder(principal.getCustomerId(), orderId));
    }
}
