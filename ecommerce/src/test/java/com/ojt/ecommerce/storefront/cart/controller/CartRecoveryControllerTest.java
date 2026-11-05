package com.ojt.ecommerce.storefront.cart.controller;

import com.ojt.ecommerce.storefront.cart.dto.RestoreToCartResponse;
import com.ojt.ecommerce.storefront.cart.service.CartService;
import com.ojt.ecommerce.storefront.exception.AccessDeniedException;
import com.ojt.ecommerce.storefront.exception.ConflictException;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartRecoveryControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartRecoveryController cartRecoveryController;

    private CustomerPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new CustomerPrincipal(1L, "customer@example.com");
    }

    @Test
    @DisplayName("restoreToCart - successfully restores failed cart order and returns 200 OK")
    void restoreToCart_success_returns200() {
        Long orderId = 500L;
        RestoreToCartResponse mockResponse = RestoreToCartResponse.builder()
                .orderId(orderId)
                .activeCartId(20L)
                .restoredItemCount(2)
                .skippedItemCount(0)
                .message("Successfully restored 2 item(s) to active cart")
                .items(Collections.emptyList())
                .build();

        when(cartService.restoreFailedCartOrder(1L, orderId)).thenReturn(mockResponse);

        ResponseEntity<RestoreToCartResponse> response = cartRecoveryController.restoreToCart(principal, orderId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(cartService, times(1)).restoreFailedCartOrder(1L, orderId);
    }

    @Test
    @DisplayName("restoreToCart - throws AccessDeniedException when customer does not own order")
    void restoreToCart_accessDenied_propagates() {
        Long orderId = 500L;
        when(cartService.restoreFailedCartOrder(1L, orderId))
                .thenThrow(new AccessDeniedException("You do not have permission to restore this order"));

        assertThrows(AccessDeniedException.class, () ->
                cartRecoveryController.restoreToCart(principal, orderId));
        verify(cartService).restoreFailedCartOrder(1L, orderId);
    }

    @Test
    @DisplayName("restoreToCart - throws ConflictException when order is already restored or paid")
    void restoreToCart_conflict_propagates() {
        Long orderId = 500L;
        when(cartService.restoreFailedCartOrder(1L, orderId))
                .thenThrow(new ConflictException("This cart order has already been restored"));

        assertThrows(ConflictException.class, () ->
                cartRecoveryController.restoreToCart(principal, orderId));
        verify(cartService).restoreFailedCartOrder(1L, orderId);
    }

    @Test
    @DisplayName("restoreToCart - throws InvalidRequestException for BUY_NOW or PENDING order")
    void restoreToCart_invalidRequest_propagates() {
        Long orderId = 500L;
        when(cartService.restoreFailedCartOrder(1L, orderId))
                .thenThrow(new InvalidRequestException("Only CART orders can be restored to active cart"));

        assertThrows(InvalidRequestException.class, () ->
                cartRecoveryController.restoreToCart(principal, orderId));
        verify(cartService).restoreFailedCartOrder(1L, orderId);
    }
}
