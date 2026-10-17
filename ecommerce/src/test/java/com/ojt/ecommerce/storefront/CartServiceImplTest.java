package com.ojt.ecommerce.storefront;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.enums.ProductStatus;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import com.ojt.ecommerce.storefront.cart.dto.*;
import com.ojt.ecommerce.storefront.cart.repository.CartItemRepository;
import com.ojt.ecommerce.storefront.cart.repository.CartRepository;
import com.ojt.ecommerce.storefront.cart.service.CartServiceImpl;
import com.ojt.ecommerce.storefront.catalog.repository.*;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private G5ProductVariantRepository variantRepository;

    @Mock
    private G5ProductImageRepository imageRepository;

    @Mock
    private G5InventoryRepository inventoryRepository;

    @Mock
    private G5VariantOptionValueRepository optionValueRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private Customer mockCustomer;
    private Product mockProduct;
    private ProductVariant mockVariant;
    private Inventory mockInventory;

    @BeforeEach
    void setUp() {
        mockCustomer = createCustomer(1L);
        mockProduct = createProduct(1L, ProductStatus.ACTIVE);
        mockVariant = createVariant(101L, mockProduct, BigDecimal.valueOf(5000), ProductVariantStatus.ACTIVE);
        mockInventory = createInventory(1L, mockVariant, 10);
    }

    // =========================================================================
    // P0 TESTS: Critical Business Logic
    // =========================================================================

    @Test
    @DisplayName("P0 - TEST 1: addItem - creates new active cart and item when no active cart exists")
    void addItem_newCart_createsActiveCartAndItem() {
        // Arrange
        Long customerId = 1L;
        AddToCartRequest request = new AddToCartRequest();
        request.setVariantId(101L);
        request.setQuantity(2);

        Cart savedCart = createCart(10L, mockCustomer);
        CartItem savedItem = createCartItem(20L, savedCart, mockVariant, 2);

        when(variantRepository.findById(101L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariantVariantId(101L)).thenReturn(Optional.of(mockInventory));
        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.empty());
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(cartRepository.save(any(Cart.class))).thenReturn(savedCart);
        when(cartItemRepository.findByCartCartIdAndVariantVariantId(10L, 101L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);

        // Act
        AddToCartResponse response = cartService.addItem(customerId, request);

        // Assert
        assertNotNull(response);
        assertEquals(20L, response.getCartItemId());
        assertEquals(10L, response.getCartId());
        assertEquals(101L, response.getVariantId());
        assertEquals(2, response.getQuantity());

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository, atLeastOnce()).save(cartCaptor.capture());
        Cart firstCartSave = cartCaptor.getAllValues().get(0);
        assertEquals(mockCustomer, firstCartSave.getCustomer());
        assertEquals("ACTIVE", firstCartSave.getStatus());
        assertEquals(0L, firstCartSave.getVersion());

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());
        CartItem capturedItem = itemCaptor.getValue();
        assertEquals(savedCart, capturedItem.getCart());
        assertEquals(mockVariant, capturedItem.getVariant());
        assertEquals(2, capturedItem.getQuantity());

        // Verify inventory is never mutated
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("P0 - TEST 2: addItem - combines quantity when adding same variant to existing cart")
    void addItem_sameVariant_combinesQuantity() {
        // Arrange
        Long customerId = 1L;
        AddToCartRequest request = new AddToCartRequest();
        request.setVariantId(101L);
        request.setQuantity(3);

        Cart activeCart = createCart(10L, mockCustomer);
        CartItem existingItem = createCartItem(20L, activeCart, mockVariant, 2);

        when(variantRepository.findById(101L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariantVariantId(101L)).thenReturn(Optional.of(mockInventory));
        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.of(activeCart));
        when(cartItemRepository.findByCartCartIdAndVariantVariantId(10L, 101L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AddToCartResponse response = cartService.addItem(customerId, request);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getQuantity());
        assertEquals(5, existingItem.getQuantity());
        assertNotNull(existingItem.getModifiedAt());

        verify(cartItemRepository).save(existingItem);
        verify(cartRepository).save(activeCart);
        verify(customerRepository, never()).findById(any());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("P0 - TEST 3: addItem - throws InvalidRequestException when requested quantity exceeds available stock")
    void addItem_insufficientStock_throws() {
        // Arrange
        Long customerId = 1L;
        AddToCartRequest request = new AddToCartRequest();
        request.setVariantId(101L);
        request.setQuantity(5);

        Inventory lowStockInventory = createInventory(1L, mockVariant, 2);

        when(variantRepository.findById(101L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariantVariantId(101L)).thenReturn(Optional.of(lowStockInventory));

        // Act & Assert
        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                cartService.addItem(customerId, request));

        assertEquals("Not enough stock available", ex.getMessage());
        verify(cartRepository, never()).save(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("P0 - TEST 4: addItem - throws InvalidRequestException when combined quantity exceeds available stock")
    void addItem_combinedQuantityExceedsStock_throws() {
        // Arrange
        Long customerId = 1L;
        AddToCartRequest request = new AddToCartRequest();
        request.setVariantId(101L);
        request.setQuantity(2);

        Inventory fourStockInventory = createInventory(1L, mockVariant, 4);
        Cart activeCart = createCart(10L, mockCustomer);
        CartItem existingItem = createCartItem(20L, activeCart, mockVariant, 3);

        when(variantRepository.findById(101L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariantVariantId(101L)).thenReturn(Optional.of(fourStockInventory));
        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.of(activeCart));
        when(cartItemRepository.findByCartCartIdAndVariantVariantId(10L, 101L)).thenReturn(Optional.of(existingItem));

        // Act & Assert
        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                cartService.addItem(customerId, request));

        assertEquals("Combined quantity exceeds stock", ex.getMessage());
        assertEquals(3, existingItem.getQuantity()); // unchanged
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("P0 - TEST 5: addItem - throws InvalidRequestException when product or variant is inactive")
    void addItem_inactiveProductOrVariant_throws() {
        // Arrange: Inactive Variant
        Long customerId = 1L;
        AddToCartRequest request = new AddToCartRequest();
        request.setVariantId(101L);
        request.setQuantity(1);

        ProductVariant inactiveVariant = createVariant(101L, mockProduct, BigDecimal.valueOf(5000), ProductVariantStatus.INACTIVE);
        when(variantRepository.findById(101L)).thenReturn(Optional.of(inactiveVariant));

        // Act & Assert for Inactive Variant
        InvalidRequestException ex1 = assertThrows(InvalidRequestException.class, () ->
                cartService.addItem(customerId, request));
        assertEquals("Product is not available", ex1.getMessage());

        // Arrange: Inactive Product
        Product inactiveProduct = createProduct(2L, ProductStatus.INACTIVE);
        ProductVariant variantWithInactiveProduct = createVariant(102L, inactiveProduct, BigDecimal.valueOf(5000), ProductVariantStatus.ACTIVE);
        when(variantRepository.findById(102L)).thenReturn(Optional.of(variantWithInactiveProduct));
        request.setVariantId(102L);

        // Act & Assert for Inactive Product
        InvalidRequestException ex2 = assertThrows(InvalidRequestException.class, () ->
                cartService.addItem(customerId, request));
        assertEquals("Product is not available", ex2.getMessage());

        // Verify downstream cart/inventory operations are never reached
        verify(cartRepository, never()).save(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("P0 - TEST 6: updateItemQuantity - updates quantity and saves cart item and cart")
    void updateItemQuantity_valid_updatesQuantity() {
        // Arrange
        Long customerId = 1L;
        Long cartItemId = 20L;
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setQuantity(4);

        Cart activeCart = createCart(10L, mockCustomer);
        CartItem cartItem = createCartItem(cartItemId, activeCart, mockVariant, 2);

        when(cartItemRepository.findByCartItemIdAndCartCustomerCustomerId(cartItemId, customerId))
                .thenReturn(Optional.of(cartItem));
        when(inventoryRepository.findByVariantVariantId(101L)).thenReturn(Optional.of(mockInventory));

        // Act
        cartService.updateItemQuantity(customerId, cartItemId, request);

        // Assert
        assertEquals(4, cartItem.getQuantity());
        assertNotNull(cartItem.getModifiedAt());
        assertNotNull(activeCart.getModifiedAt());

        verify(cartItemRepository).save(cartItem);
        verify(cartRepository).save(activeCart);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("P0 - TEST 7: updateItemQuantity - throws ResourceNotFoundException when item does not belong to customer")
    void updateItemQuantity_wrongCustomer_throws() {
        // Arrange
        Long customerId = 999L;
        Long cartItemId = 20L;
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setQuantity(4);

        when(cartItemRepository.findByCartItemIdAndCartCustomerCustomerId(cartItemId, customerId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                cartService.updateItemQuantity(customerId, cartItemId, request));

        assertEquals("Cart item not found", ex.getMessage());
        verify(inventoryRepository, never()).findByVariantVariantId(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("P0 - TEST 8: updateItemQuantity - throws InvalidRequestException when requested quantity exceeds available stock")
    void updateItemQuantity_exceedsStock_throws() {
        // Arrange
        Long customerId = 1L;
        Long cartItemId = 20L;
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setQuantity(5);

        Cart activeCart = createCart(10L, mockCustomer);
        CartItem cartItem = createCartItem(cartItemId, activeCart, mockVariant, 2);
        Inventory stockThree = createInventory(1L, mockVariant, 3);

        when(cartItemRepository.findByCartItemIdAndCartCustomerCustomerId(cartItemId, customerId))
                .thenReturn(Optional.of(cartItem));
        when(inventoryRepository.findByVariantVariantId(101L)).thenReturn(Optional.of(stockThree));

        // Act & Assert
        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                cartService.updateItemQuantity(customerId, cartItemId, request));

        assertEquals("Quantity exceeds available stock", ex.getMessage());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("P0 - TEST 9: removeItem - deletes cart item and updates cart modifiedAt")
    void removeItem_valid_deletesItem() {
        // Arrange
        Long customerId = 1L;
        Long cartItemId = 20L;

        Cart activeCart = createCart(10L, mockCustomer);
        CartItem cartItem = createCartItem(cartItemId, activeCart, mockVariant, 2);

        when(cartItemRepository.findByCartItemIdAndCartCustomerCustomerId(cartItemId, customerId))
                .thenReturn(Optional.of(cartItem));

        // Act
        cartService.removeItem(customerId, cartItemId);

        // Assert
        verify(cartItemRepository).delete(cartItem);
        assertNotNull(activeCart.getModifiedAt());
        verify(cartRepository).save(activeCart);
    }

    @Test
    @DisplayName("P0 - TEST 10: removeItem - throws ResourceNotFoundException when item does not belong to customer")
    void removeItem_wrongCustomer_throws() {
        // Arrange
        Long customerId = 999L;
        Long cartItemId = 20L;

        when(cartItemRepository.findByCartItemIdAndCartCustomerCustomerId(cartItemId, customerId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                cartService.removeItem(customerId, cartItemId));

        assertEquals("Cart item not found", ex.getMessage());
        verify(cartItemRepository, never()).delete(any());
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("P0 - TEST 11: mergeGuestCart - clamps quantity to available stock when combined exceeds stock")
    void mergeGuestCart_exceedsStock_actualBehavior() {
        // Arrange
        Long customerId = 1L;
        GuestCartItem guestItem = new GuestCartItem();
        guestItem.setVariantId(101L);
        guestItem.setQuantity(5);

        CartMergeRequest request = new CartMergeRequest();
        request.setItems(List.of(guestItem));

        Cart activeCart = createCart(10L, mockCustomer);
        CartItem dbCartItem = createCartItem(20L, activeCart, mockVariant, 2);
        Inventory stockFour = createInventory(1L, mockVariant, 4);

        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.of(activeCart));
        when(variantRepository.findById(101L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariantVariantId(101L)).thenReturn(Optional.of(stockFour));
        when(cartItemRepository.findByCartCartIdAndVariantVariantId(10L, 101L)).thenReturn(Optional.of(dbCartItem));

        // Act
        CartMergeResponse response = cartService.mergeGuestCart(customerId, request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getMergedVariantIds().size());
        assertTrue(response.getMergedVariantIds().contains(101L));

        assertEquals(4, dbCartItem.getQuantity()); // Clamped to available stock
        assertNotNull(dbCartItem.getModifiedAt());
        verify(cartItemRepository).save(dbCartItem);

        assertEquals(1, response.getRejectedItems().size());
        CartMergeRejectedItem rejectedItem = response.getRejectedItems().get(0);
        assertEquals(101L, rejectedItem.getVariantId());
        assertEquals("Combined quantity exceeded stock, clamped to available", rejectedItem.getReason());

        assertNotNull(activeCart.getModifiedAt());
        verify(cartRepository).save(activeCart);
    }

    // =========================================================================
    // P1 TESTS: Additional Flow & Quality Scenarios
    // =========================================================================

    @Test
    @DisplayName("P1 - TEST 12: addItem - reuses existing cart and adds new CartItem when variant is new")
    void addItem_existingCart_newVariant_addsItem() {
        // Arrange
        Long customerId = 1L;
        AddToCartRequest request = new AddToCartRequest();
        request.setVariantId(102L);
        request.setQuantity(2);

        Cart activeCart = createCart(10L, mockCustomer);
        ProductVariant variant2 = createVariant(102L, mockProduct, BigDecimal.valueOf(3000), ProductVariantStatus.ACTIVE);
        Inventory inventory2 = createInventory(2L, variant2, 8);
        CartItem savedNewItem = createCartItem(21L, activeCart, variant2, 2);

        when(variantRepository.findById(102L)).thenReturn(Optional.of(variant2));
        when(inventoryRepository.findByVariantVariantId(102L)).thenReturn(Optional.of(inventory2));
        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.of(activeCart));
        when(cartItemRepository.findByCartCartIdAndVariantVariantId(10L, 102L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedNewItem);

        // Act
        AddToCartResponse response = cartService.addItem(customerId, request);

        // Assert
        assertNotNull(response);
        assertEquals(21L, response.getCartItemId());
        assertEquals(10L, response.getCartId());
        assertEquals(102L, response.getVariantId());
        assertEquals(2, response.getQuantity());

        // Verify customerRepository is NEVER queried (existing cart reused)
        verify(customerRepository, never()).findById(any());

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());
        CartItem capturedItem = itemCaptor.getValue();
        assertEquals(activeCart, capturedItem.getCart());
        assertEquals(variant2, capturedItem.getVariant());
        assertEquals(2, capturedItem.getQuantity());
    }

    @Test
    @DisplayName("P1 - TEST 13: getCart - maps items, options, primary images, and correctly calculates totals")
    void getCart_mapsItemsAndTotals() {
        // Arrange
        Long customerId = 1L;
        Cart activeCart = createCart(10L, mockCustomer);

        Product product1 = createProduct(1L, ProductStatus.ACTIVE);
        ProductVariant variant1 = createVariant(101L, product1, BigDecimal.valueOf(1000), ProductVariantStatus.ACTIVE);
        CartItem item1 = createCartItem(21L, activeCart, variant1, 2);

        Product product2 = createProduct(2L, ProductStatus.ACTIVE);
        ProductVariant variant2 = createVariant(102L, product2, BigDecimal.valueOf(500), ProductVariantStatus.ACTIVE);
        CartItem item2 = createCartItem(22L, activeCart, variant2, 1);

        Inventory inv1 = createInventory(1L, variant1, 10);
        Inventory inv2 = createInventory(2L, variant2, 5);

        ProductImage img1 = ProductImage.builder().imageId(1L).product(product1).imageUrl("http://img1.png").isPrimary(true).build();
        ProductImage img2 = ProductImage.builder().imageId(2L).product(product2).imageUrl("http://img2.png").isPrimary(true).build();

        Variation colorVariation = Variation.builder().variationId(1L).name("Color").build();
        VariationOption redOption = VariationOption.builder().optionId(1L).variation(colorVariation).value("Red").build();
        VariantOptionValue optVal1 = VariantOptionValue.builder().id(1L).variant(variant1).option(redOption).build();

        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.of(activeCart));
        when(cartItemRepository.findByCartCartId(10L)).thenReturn(List.of(item1, item2));
        when(inventoryRepository.findByVariantVariantIdIn(anyList())).thenReturn(List.of(inv1, inv2));
        when(imageRepository.findByProductProductIdIn(anyList())).thenReturn(List.of(img1, img2));
        when(optionValueRepository.findByVariantVariantIdIn(anyList())).thenReturn(List.of(optVal1));

        // Act
        CartResponse response = cartService.getCart(customerId);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.getCartId());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(3, response.getTotalQuantity()); // 2 + 1
        assertEquals(0, BigDecimal.valueOf(2500).compareTo(response.getTotalAmount())); // (1000*2) + (500*1)
        assertEquals(2, response.getItems().size());

        CartItemResponse rItem1 = response.getItems().get(0);
        assertEquals(21L, rItem1.getCartItemId());
        assertEquals(101L, rItem1.getVariantId());
        assertEquals(2, rItem1.getQuantity());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(rItem1.getEffectivePrice()));
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(rItem1.getSubtotal()));
        assertEquals(10, rItem1.getAvailableQuantity());
        assertEquals("IN_STOCK", rItem1.getStockStatus());
        assertTrue(rItem1.getAvailable());
        assertEquals("http://img1.png", rItem1.getImageUrl());
        assertEquals("Red", rItem1.getOptions().get("Color"));

        CartItemResponse rItem2 = response.getItems().get(1);
        assertEquals(22L, rItem2.getCartItemId());
        assertEquals(102L, rItem2.getVariantId());
        assertEquals(1, rItem2.getQuantity());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(rItem2.getEffectivePrice()));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(rItem2.getSubtotal()));
        assertEquals(5, rItem2.getAvailableQuantity());
        assertEquals("IN_STOCK", rItem2.getStockStatus());
        assertTrue(rItem2.getAvailable());
        assertEquals("http://img2.png", rItem2.getImageUrl());
    }

    @Test
    @DisplayName("P1 - TEST 14: updateItemQuantity - delegates to removeItem when quantity is zero or negative")
    void updateItemQuantity_zeroOrNegative_behavior() {
        // Arrange
        Long customerId = 1L;
        Long cartItemId = 20L;
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setQuantity(0);

        Cart activeCart = createCart(10L, mockCustomer);
        CartItem cartItem = createCartItem(cartItemId, activeCart, mockVariant, 2);

        when(cartItemRepository.findByCartItemIdAndCartCustomerCustomerId(cartItemId, customerId))
                .thenReturn(Optional.of(cartItem));

        // Act
        cartService.updateItemQuantity(customerId, cartItemId, request);

        // Assert
        verify(cartItemRepository).delete(cartItem);
        verify(cartItemRepository, never()).save(any());
        assertNotNull(activeCart.getModifiedAt());
        verify(cartRepository).save(activeCart);
    }

    @Test
    @DisplayName("P1 - TEST 15: resolveGuestCart - maps live price, stock status and totals for active items")
    void resolveGuestCart_mapsLivePriceAndStock() {
        // Arrange
        GuestCartItem guestItem = new GuestCartItem();
        guestItem.setVariantId(101L);
        guestItem.setQuantity(2);

        GuestCartResolveRequest request = new GuestCartResolveRequest();
        request.setItems(List.of(guestItem));

        when(variantRepository.findAllById(List.of(101L))).thenReturn(List.of(mockVariant));
        when(inventoryRepository.findByVariantVariantIdIn(List.of(101L))).thenReturn(List.of(mockInventory));
        when(imageRepository.findByProductProductIdIn(anyList())).thenReturn(Collections.emptyList());
        when(optionValueRepository.findByVariantVariantIdIn(anyList())).thenReturn(Collections.emptyList());

        // Act
        GuestCartResolveResponse response = cartService.resolveGuestCart(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTotalQuantity());
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(response.getTotalAmount()));
        assertEquals(1, response.getItems().size());

        GuestCartResolvedItemResponse itemResp = response.getItems().get(0);
        assertEquals(101L, itemResp.getVariantId());
        assertEquals(2, itemResp.getQuantity());
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(itemResp.getEffectivePrice()));
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(itemResp.getSubtotal()));
        assertEquals(10, itemResp.getAvailableQuantity());
        assertEquals("IN_STOCK", itemResp.getStockStatus());
        assertTrue(itemResp.getAvailable());
    }

    @Test
    @DisplayName("P1 - TEST 16: resolveGuestCart - marks out of stock item unavailable and excludes it from totals")
    void resolveGuestCart_outOfStock_marksUnavailable() {
        // Arrange
        GuestCartItem guestItem = new GuestCartItem();
        guestItem.setVariantId(101L);
        guestItem.setQuantity(2);

        GuestCartResolveRequest request = new GuestCartResolveRequest();
        request.setItems(List.of(guestItem));

        Inventory zeroStockInventory = createInventory(1L, mockVariant, 0);

        when(variantRepository.findAllById(List.of(101L))).thenReturn(List.of(mockVariant));
        when(inventoryRepository.findByVariantVariantIdIn(List.of(101L))).thenReturn(List.of(zeroStockInventory));
        when(imageRepository.findByProductProductIdIn(anyList())).thenReturn(Collections.emptyList());
        when(optionValueRepository.findByVariantVariantIdIn(anyList())).thenReturn(Collections.emptyList());

        // Act
        GuestCartResolveResponse response = cartService.resolveGuestCart(request);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalQuantity()); // Excluded from total
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalAmount())); // Excluded from total
        assertEquals(1, response.getItems().size());

        GuestCartResolvedItemResponse itemResp = response.getItems().get(0);
        assertEquals(101L, itemResp.getVariantId());
        assertEquals(0, itemResp.getAvailableQuantity());
        assertEquals("OUT_OF_STOCK", itemResp.getStockStatus());
        assertFalse(itemResp.getAvailable());
    }

    @Test
    @DisplayName("P1 - TEST 17: mergeGuestCart - saves new CartItem when guest item does not exist in DB cart")
    void mergeGuestCart_newItem_merges() {
        // Arrange
        Long customerId = 1L;
        GuestCartItem guestItem = new GuestCartItem();
        guestItem.setVariantId(101L);
        guestItem.setQuantity(2);

        CartMergeRequest request = new CartMergeRequest();
        request.setItems(List.of(guestItem));

        Cart activeCart = createCart(10L, mockCustomer);

        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.of(activeCart));
        when(variantRepository.findById(101L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariantVariantId(101L)).thenReturn(Optional.of(mockInventory));
        when(cartItemRepository.findByCartCartIdAndVariantVariantId(10L, 101L)).thenReturn(Optional.empty());

        // Act
        CartMergeResponse response = cartService.mergeGuestCart(customerId, request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getMergedVariantIds().size());
        assertTrue(response.getMergedVariantIds().contains(101L));
        assertTrue(response.getRejectedItems().isEmpty());

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());
        CartItem capturedItem = itemCaptor.getValue();
        assertEquals(activeCart, capturedItem.getCart());
        assertEquals(mockVariant, capturedItem.getVariant());
        assertEquals(2, capturedItem.getQuantity());

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("P1 - TEST 18: mergeGuestCart - combines quantity when guest item already exists in DB cart")
    void mergeGuestCart_existingItem_combinesQuantity() {
        // Arrange
        Long customerId = 1L;
        GuestCartItem guestItem = new GuestCartItem();
        guestItem.setVariantId(101L);
        guestItem.setQuantity(3);

        CartMergeRequest request = new CartMergeRequest();
        request.setItems(List.of(guestItem));

        Cart activeCart = createCart(10L, mockCustomer);
        CartItem existingItem = createCartItem(20L, activeCart, mockVariant, 2);

        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.of(activeCart));
        when(variantRepository.findById(101L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariantVariantId(101L)).thenReturn(Optional.of(mockInventory)); // stock = 10
        when(cartItemRepository.findByCartCartIdAndVariantVariantId(10L, 101L)).thenReturn(Optional.of(existingItem));

        // Act
        CartMergeResponse response = cartService.mergeGuestCart(customerId, request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getMergedVariantIds().size());
        assertTrue(response.getMergedVariantIds().contains(101L));
        assertTrue(response.getRejectedItems().isEmpty());

        assertEquals(5, existingItem.getQuantity()); // 2 + 3
        verify(cartItemRepository).save(existingItem);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("P1 - TEST 19: mergeGuestCart - rejects item when stock is zero")
    void mergeGuestCart_outOfStock_actualBehavior() {
        // Arrange
        Long customerId = 1L;
        GuestCartItem guestItem = new GuestCartItem();
        guestItem.setVariantId(101L);
        guestItem.setQuantity(2);

        CartMergeRequest request = new CartMergeRequest();
        request.setItems(List.of(guestItem));

        Cart activeCart = createCart(10L, mockCustomer);
        Inventory zeroStockInventory = createInventory(1L, mockVariant, 0);

        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.of(activeCart));
        when(variantRepository.findById(101L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariantVariantId(101L)).thenReturn(Optional.of(zeroStockInventory));

        // Act
        CartMergeResponse response = cartService.mergeGuestCart(customerId, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getMergedVariantIds().isEmpty());
        assertEquals(1, response.getRejectedItems().size());

        CartMergeRejectedItem rejectedItem = response.getRejectedItems().get(0);
        assertEquals(101L, rejectedItem.getVariantId());
        assertEquals("Out of stock", rejectedItem.getReason());

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("P1 - TEST 20: cartOperations_doNotModifyInventory - proves inventory is never mutated by cart operations")
    void cartOperations_doNotModifyInventory() {
        // Arrange
        Long customerId = 1L;
        AddToCartRequest request = new AddToCartRequest();
        request.setVariantId(101L);
        request.setQuantity(2);

        Cart savedCart = createCart(10L, mockCustomer);
        CartItem savedItem = createCartItem(20L, savedCart, mockVariant, 2);

        when(variantRepository.findById(101L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariantVariantId(101L)).thenReturn(Optional.of(mockInventory));
        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.of(savedCart));
        when(cartItemRepository.findByCartCartIdAndVariantVariantId(10L, 101L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);

        // Act
        cartService.addItem(customerId, request);

        // Assert
        verify(inventoryRepository, never()).save(any());
        verify(inventoryRepository, never()).delete(any());
        verify(inventoryRepository, never()).deleteAll(any());
    }

    // =========================================================================
    // P2 TESTS: Edge Cases & Boundary Conditions
    // =========================================================================

    @Test
    @DisplayName("P2 - TEST 21: getCart - returns zero totals and empty items when cart does not exist or has no items")
    void getCart_empty_returnsZeroTotals() {
        // Scenario A: No active cart exists in database
        Long customerId = 1L;
        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.empty());

        CartResponse emptyResponseA = cartService.getCart(customerId);

        assertNotNull(emptyResponseA);
        assertNull(emptyResponseA.getCartId());
        assertEquals("ACTIVE", emptyResponseA.getStatus());
        assertEquals(0, emptyResponseA.getTotalQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(emptyResponseA.getTotalAmount()));
        assertTrue(emptyResponseA.getItems().isEmpty());

        // Scenario B: Active cart exists but contains no cart items
        Cart activeCart = createCart(10L, mockCustomer);
        when(cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")).thenReturn(Optional.of(activeCart));
        when(cartItemRepository.findByCartCartId(10L)).thenReturn(Collections.emptyList());

        CartResponse emptyResponseB = cartService.getCart(customerId);

        assertNotNull(emptyResponseB);
        assertEquals(10L, emptyResponseB.getCartId());
        assertEquals("ACTIVE", emptyResponseB.getStatus());
        assertEquals(0, emptyResponseB.getTotalQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(emptyResponseB.getTotalAmount()));
        assertTrue(emptyResponseB.getItems().isEmpty());
    }

    // =========================================================================
    // PRIVATE TEST HELPERS (Classes & Entities Inside This File Only)
    // =========================================================================

    private Customer createCustomer(Long customerId) {
        return Customer.builder()
                .customerId(customerId)
                .fullName("Test Customer " + customerId)
                .email("customer" + customerId + "@example.com")
                .status("ACTIVE")
                .build();
    }

    private Product createProduct(Long productId, ProductStatus status) {
        return Product.builder()
                .productId(productId)
                .productName("Test Product " + productId)
                .status(status)
                .build();
    }

    private ProductVariant createVariant(Long variantId, Product product, BigDecimal price, ProductVariantStatus status) {
        return ProductVariant.builder()
                .variantId(variantId)
                .product(product)
                .sku("SKU-" + variantId)
                .sellingPrice(price)
                .costPrice(price.multiply(BigDecimal.valueOf(0.8)))
                .status(status)
                .build();
    }

    private Inventory createInventory(Long inventoryId, ProductVariant variant, int quantity) {
        return Inventory.builder()
                .inventoryId(inventoryId)
                .variant(variant)
                .quantity(quantity)
                .reservedQuantity(0)
                .reorderLevel(5)
                .status(quantity > 0 ? "NORMAL" : "OUT_OF_STOCK")
                .build();
    }

    private Cart createCart(Long cartId, Customer customer) {
        return Cart.builder()
                .cartId(cartId)
                .customer(customer)
                .status("ACTIVE")
                .version(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CartItem createCartItem(Long cartItemId, Cart cart, ProductVariant variant, int quantity) {
        return CartItem.builder()
                .cartItemId(cartItemId)
                .cart(cart)
                .variant(variant)
                .quantity(quantity)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
