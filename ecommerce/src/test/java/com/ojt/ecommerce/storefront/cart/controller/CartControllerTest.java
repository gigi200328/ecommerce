package com.ojt.ecommerce.storefront.cart.controller;

import com.ojt.ecommerce.storefront.cart.dto.*;
import com.ojt.ecommerce.storefront.cart.service.CartService;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private CustomerPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new CustomerPrincipal(1L, "customer@example.com");
    }

    @Test
    @DisplayName("1. getCart - returns 200 OK with CartResponse when authenticated")
    void getCart_returns200WithCartResponse() {
        CartResponse mockResponse = CartResponse.builder()
                .cartId(10L)
                .status("ACTIVE")
                .totalQuantity(2)
                .totalAmount(BigDecimal.valueOf(15000))
                .items(Collections.emptyList())
                .build();

        when(cartService.getCart(1L)).thenReturn(mockResponse);

        ResponseEntity<CartResponse> response = cartController.getCart(principal);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(cartService, times(1)).getCart(1L);
    }

    @Test
    @DisplayName("2. addItem - returns 200 OK with AddToCartResponse when request is valid")
    void addItem_returns200WithAddToCartResponse() {
        AddToCartRequest request = new AddToCartRequest();
        request.setVariantId(101L);
        request.setQuantity(2);

        AddToCartResponse mockResponse = AddToCartResponse.builder()
                .cartItemId(20L)
                .cartId(10L)
                .variantId(101L)
                .quantity(2)
                .build();

        when(cartService.addItem(1L, request)).thenReturn(mockResponse);

        ResponseEntity<AddToCartResponse> response = cartController.addItem(principal, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(cartService, times(1)).addItem(1L, request);
    }

    @Test
    @DisplayName("3. addItem - propagates InvalidRequestException when service validation fails")
    void addItem_serviceExceptionPropagation() {
        AddToCartRequest request = new AddToCartRequest();
        request.setVariantId(101L);
        request.setQuantity(999);

        when(cartService.addItem(1L, request))
                .thenThrow(new InvalidRequestException("Not enough stock available"));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> cartController.addItem(principal, request));

        assertEquals("Not enough stock available", ex.getMessage());
        verify(cartService, times(1)).addItem(1L, request);
    }

    @Test
    @DisplayName("4. updateItemQuantity - returns 204 No Content when request is valid")
    void updateItemQuantity_returns204NoContent() {
        Long cartItemId = 20L;
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setQuantity(3);

        doNothing().when(cartService).updateItemQuantity(1L, cartItemId, request);

        ResponseEntity<Void> response = cartController.updateItemQuantity(principal, cartItemId, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(cartService, times(1)).updateItemQuantity(1L, cartItemId, request);
    }

    @Test
    @DisplayName("5. updateItemQuantity - propagates ResourceNotFoundException when item not found")
    void updateItemQuantity_missingItemException() {
        Long cartItemId = 999L;
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setQuantity(3);

        doThrow(new ResourceNotFoundException("Cart item not found"))
                .when(cartService).updateItemQuantity(1L, cartItemId, request);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> cartController.updateItemQuantity(principal, cartItemId, request));

        assertEquals("Cart item not found", ex.getMessage());
        verify(cartService, times(1)).updateItemQuantity(1L, cartItemId, request);
    }

    @Test
    @DisplayName("6. removeItem - returns 204 No Content when item is removed successfully")
    void removeItem_returns204NoContent() {
        Long cartItemId = 20L;

        doNothing().when(cartService).removeItem(1L, cartItemId);

        ResponseEntity<Void> response = cartController.removeItem(principal, cartItemId);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(cartService, times(1)).removeItem(1L, cartItemId);
    }

    @Test
    @DisplayName("7. resolveGuestCart - returns 200 OK with GuestCartResolveResponse")
    void resolveGuestCart_returns200WithGuestCartResolveResponse() {
        GuestCartResolveRequest request = new GuestCartResolveRequest();
        GuestCartResolveResponse mockResponse = GuestCartResolveResponse.builder()
                .totalQuantity(1)
                .totalAmount(BigDecimal.valueOf(5000))
                .items(Collections.emptyList())
                .build();

        when(cartService.resolveGuestCart(request)).thenReturn(mockResponse);

        ResponseEntity<GuestCartResolveResponse> response = cartController.resolveGuestCart(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(cartService, times(1)).resolveGuestCart(request);
    }

    @Test
    @DisplayName("8. mergeGuestCart - returns 200 OK with CartMergeResponse when authenticated")
    void mergeGuestCart_returns200WithCartMergeResponse() {
        CartMergeRequest request = new CartMergeRequest();
        CartMergeResponse mockResponse = CartMergeResponse.builder()
                .mergedVariantIds(List.of(101L))
                .rejectedItems(Collections.emptyList())
                .build();

        when(cartService.mergeGuestCart(1L, request)).thenReturn(mockResponse);

        ResponseEntity<CartMergeResponse> response = cartController.mergeGuestCart(principal, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(cartService, times(1)).mergeGuestCart(1L, request);
    }
}
