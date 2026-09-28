package com.ojt.ecommerce.storefront.cart.controller;

import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import com.ojt.ecommerce.storefront.cart.dto.*;
import com.ojt.ecommerce.storefront.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal CustomerPrincipal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getCustomerId()));
    }

    @PostMapping("/items")
    public ResponseEntity<AddToCartResponse> addItem(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItem(principal.getCustomerId(), request));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<Void> updateItemQuantity(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        cartService.updateItemQuantity(principal.getCustomerId(), cartItemId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable Long cartItemId) {
        cartService.removeItem(principal.getCustomerId(), cartItemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/guest/resolve")
    public ResponseEntity<GuestCartResolveResponse> resolveGuestCart(@RequestBody GuestCartResolveRequest request) {
        return ResponseEntity.ok(cartService.resolveGuestCart(request));
    }

    @PostMapping("/merge")
    public ResponseEntity<CartMergeResponse> mergeGuestCart(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @RequestBody CartMergeRequest request) {
        return ResponseEntity.ok(cartService.mergeGuestCart(principal.getCustomerId(), request));
    }
}
