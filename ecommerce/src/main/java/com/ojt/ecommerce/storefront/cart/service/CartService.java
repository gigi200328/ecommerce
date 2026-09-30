package com.ojt.ecommerce.storefront.cart.service;
import com.ojt.ecommerce.storefront.cart.dto.*;
public interface CartService {
    AddToCartResponse addItem(Long customerId, AddToCartRequest request);
    CartResponse getCart(Long customerId);
    void updateItemQuantity(Long customerId, Long cartItemId, UpdateCartItemQuantityRequest request);
    void removeItem(Long customerId, Long cartItemId);
    GuestCartResolveResponse resolveGuestCart(GuestCartResolveRequest request);
    CartMergeResponse mergeGuestCart(Long customerId, CartMergeRequest request);
}