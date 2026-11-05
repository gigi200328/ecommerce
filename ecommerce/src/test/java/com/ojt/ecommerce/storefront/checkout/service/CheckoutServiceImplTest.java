package com.ojt.ecommerce.storefront.checkout.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.enums.ProductStatus;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import com.ojt.ecommerce.storefront.cart.repository.CartItemRepository;
import com.ojt.ecommerce.storefront.cart.repository.CartRepository;
import com.ojt.ecommerce.storefront.catalog.repository.G5InventoryRepository;
import com.ojt.ecommerce.storefront.catalog.repository.G5ProductVariantRepository;
import com.ojt.ecommerce.storefront.catalog.repository.G5VariantOptionValueRepository;
import com.ojt.ecommerce.storefront.checkout.dto.BuyNowItemRequest;
import com.ojt.ecommerce.storefront.checkout.dto.CreateOrderRequest;
import com.ojt.ecommerce.storefront.checkout.dto.CustomShippingAddressRequest;
import com.ojt.ecommerce.storefront.checkout.dto.OrderResponse;
import com.ojt.ecommerce.storefront.checkout.dto.ShippingQuoteResponse;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderAddressRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderItemRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.profile.repository.CustomerAddressRepository;
import com.ojt.ecommerce.storefront.reservation.service.InventoryReservationService;
import com.ojt.ecommerce.storefront.shipping.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckoutServiceImplTest {

    @Mock
    private G5OrderRepository orderRepository;

    @Mock
    private G5OrderItemRepository orderItemRepository;

    @Mock
    private G5OrderAddressRepository orderAddressRepository;

    @Mock
    private G5OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerAddressRepository customerAddressRepository;

    @Mock
    private G5InventoryRepository inventoryRepository;

    @Mock
    private G5VariantOptionValueRepository optionValueRepository;

    @Mock
    private G5ProductVariantRepository variantRepository;

    @Mock
    private ShippingService shippingService;

    @Mock
    private InventoryReservationService reservationService;

    @InjectMocks
    private CheckoutServiceImpl checkoutService;

    private Customer customer;
    private Cart cart;
    private Product product;
    private ProductVariant variant;
    private CartItem cartItem;
    private Inventory inventory;
    private CustomerAddress savedAddress;
    private ShippingQuoteResponse shippingQuote;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerId(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .build();

        cart = Cart.builder()
                .cartId(10L)
                .customer(customer)
                .status("ACTIVE")
                .build();

        product = Product.builder()
                .productId(100L)
                .productName("Cotton T-Shirt")
                .status(ProductStatus.ACTIVE)
                .build();

        variant = ProductVariant.builder()
                .variantId(200L)
                .product(product)
                .sku("TSHIRT-BLK-M")
                .sellingPrice(new BigDecimal("15000.00"))
                .status(ProductVariantStatus.ACTIVE)
                .build();

        cartItem = CartItem.builder()
                .cartItemId(500L)
                .cart(cart)
                .variant(variant)
                .quantity(2)
                .build();

        inventory = Inventory.builder()
                .inventoryId(300L)
                .variant(variant)
                .quantity(10)
                .build();

        savedAddress = CustomerAddress.builder()
                .addressId(50L)
                .customer(customer)
                .recipientName("John Doe")
                .phoneNumber("09123456789")
                .addressLine1("No. 12, Main Street")
                .city("Yangon")
                .township("Kyauktada")
                .regionOrState("Yangon Region")
                .isDefault(true)
                .build();

        shippingQuote = ShippingQuoteResponse.builder()
                .zoneId(1L)
                .city("Yangon")
                .township("Kyauktada")
                .shippingFee(new BigDecimal("2500.00"))
                .estimatedDays(2)
                .build();

        lenient().when(reservationService.getAvailableStock(anyLong())).thenReturn(10);
    }

    // =========================================================================
    // 1. Success Scenarios
    // =========================================================================

    @Test
    @DisplayName("Should successfully create order with saved customer address")
    void createOrder_WithSavedAddress_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSavedAddressId(50L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(10L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByVariantVariantId(200L)).thenReturn(Optional.of(inventory));
        when(customerAddressRepository.findByAddressIdAndCustomerCustomerId(50L, 1L)).thenReturn(Optional.of(savedAddress));
        when(shippingService.getShippingQuote(1L, 50L)).thenReturn(shippingQuote);
        when(optionValueRepository.findByVariantVariantId(200L)).thenReturn(Collections.emptyList());

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setOrderId(1001L);
            return o;
        });

        OrderResponse response = checkoutService.createOrder(1L, request);

        assertNotNull(response);
        assertEquals(1001L, response.getOrderId());
        assertNotNull(response.getOrderNo());
        assertTrue(response.getOrderNo().startsWith("ORD-"));
        assertEquals(new BigDecimal("30000.00"), response.getSubtotalAmount()); // 15000 * 2
        assertEquals(new BigDecimal("2500.00"), response.getShippingFee());
        assertEquals(new BigDecimal("32500.00"), response.getTotalAmount()); // 30000 + 2500
        assertEquals("PENDING", response.getOrderStatus());
        assertEquals("PENDING", response.getPaymentStatus());

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderAddressRepository, times(1)).save(any(OrderAddress.class));
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
        verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Should successfully create order with custom shipping address")
    void createOrder_WithCustomShippingAddress_Success() {
        CustomShippingAddressRequest customAddress = new CustomShippingAddressRequest();
        customAddress.setRecipientName("Jane Doe");
        customAddress.setPhoneNumber("09987654321");
        customAddress.setAddressLine1("Street 50");
        customAddress.setCity("Mandalay");
        customAddress.setTownship("Chanayethazan");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress(customAddress);

        ShippingQuoteResponse customQuote = ShippingQuoteResponse.builder()
                .shippingFee(new BigDecimal("4000.00"))
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(10L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByVariantVariantId(200L)).thenReturn(Optional.of(inventory));
        when(shippingService.getCustomShippingQuote(any())).thenReturn(customQuote);
        when(optionValueRepository.findByVariantVariantId(200L)).thenReturn(Collections.emptyList());

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setOrderId(1002L);
            return o;
        });

        OrderResponse response = checkoutService.createOrder(1L, request);

        assertNotNull(response);
        assertEquals(1002L, response.getOrderId());
        assertEquals(new BigDecimal("4000.00"), response.getShippingFee());
        assertEquals(new BigDecimal("34000.00"), response.getTotalAmount()); // 30000 + 4000

        verify(shippingService, times(1)).getCustomShippingQuote(any());
        verify(customerAddressRepository, never()).findByAddressIdAndCustomerCustomerId(anyLong(), anyLong());
    }

    // =========================================================================
    // 2. Exception / Failure Scenarios
    // =========================================================================

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer is not found")
    void createOrder_ThrowsException_WhenCustomerNotFound() {
        CreateOrderRequest request = new CreateOrderRequest();
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> checkoutService.createOrder(99L, request));

        assertEquals("Customer not found", ex.getMessage());
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when cart is not found or not ACTIVE")
    void createOrder_ThrowsException_WhenCartNotFound() {
        CreateOrderRequest request = new CreateOrderRequest();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.empty());

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> checkoutService.createOrder(1L, request));

        assertEquals("Empty cart", ex.getMessage());
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when cart items are empty")
    void createOrder_ThrowsException_WhenCartItemsEmpty() {
        CreateOrderRequest request = new CreateOrderRequest();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(10L)).thenReturn(Collections.emptyList());

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> checkoutService.createOrder(1L, request));

        assertEquals("Empty cart", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when variant is not ACTIVE")
    void createOrder_ThrowsException_WhenVariantInactive() {
        variant.setStatus(ProductVariantStatus.INACTIVE);

        CreateOrderRequest request = new CreateOrderRequest();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(10L)).thenReturn(List.of(cartItem));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> checkoutService.createOrder(1L, request));

        assertTrue(ex.getMessage().contains("Product or variant is no longer available"));
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when inventory is insufficient")
    void createOrder_ThrowsException_WhenStockInsufficient() {
        inventory.setQuantity(1); // Cart needs 2
        when(reservationService.getAvailableStock(200L)).thenReturn(1);

        CreateOrderRequest request = new CreateOrderRequest();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(10L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByVariantVariantId(200L)).thenReturn(Optional.of(inventory));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> checkoutService.createOrder(1L, request));

        assertTrue(ex.getMessage().contains("Insufficient stock for product"));
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when neither saved address nor custom address is provided")
    void createOrder_ThrowsException_WhenNoShippingAddressProvided() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSavedAddressId(null);
        request.setShippingAddress(null);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(10L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByVariantVariantId(200L)).thenReturn(Optional.of(inventory));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> checkoutService.createOrder(1L, request));

        assertEquals("Shipping address is required", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when saved address is not found")
    void createOrder_ThrowsException_WhenSavedAddressNotFound() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSavedAddressId(999L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(10L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByVariantVariantId(200L)).thenReturn(Optional.of(inventory));
        when(customerAddressRepository.findByAddressIdAndCustomerCustomerId(999L, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> checkoutService.createOrder(1L, request));

        assertEquals("Saved address not found", ex.getMessage());
    }

    // =========================================================================
    // 3. Phase 2 Lifecycle Tests
    // =========================================================================

    @Test
    @DisplayName("Phase 2 - Test 1, 2, 6: CART checkout links Order to Cart A, sets PENDING_PAYMENT, and creates correct OrderItem snapshot")
    void createOrder_CartCheckout_LinksOrderToCartAndTransitionsStatusToPendingPayment() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSavedAddressId(50L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(10L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByVariantVariantId(200L)).thenReturn(Optional.of(inventory));
        when(customerAddressRepository.findByAddressIdAndCustomerCustomerId(50L, 1L)).thenReturn(Optional.of(savedAddress));
        when(shippingService.getShippingQuote(1L, 50L)).thenReturn(shippingQuote);
        when(optionValueRepository.findByVariantVariantId(200L)).thenReturn(Collections.emptyList());

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setOrderId(1001L);
            return o;
        });

        OrderResponse response = checkoutService.createOrder(1L, request);

        assertNotNull(response);

        // Test 1: CART checkout links Order to Cart A
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertNotNull(capturedOrder.getCart(), "Order must link to Cart A");
        assertEquals(10L, capturedOrder.getCart().getCartId(), "Order cart_id must match Cart A cart_id");
        assertEquals("CART", capturedOrder.getCheckoutType());

        // Test 2: CART checkout changes Cart A status to PENDING_PAYMENT and saves it
        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository, times(1)).save(cartCaptor.capture());
        Cart capturedCart = cartCaptor.getValue();
        assertEquals("PENDING_PAYMENT", capturedCart.getStatus(), "Cart A status must transition to PENDING_PAYMENT");
        assertEquals(cart, capturedCart);

        // Test 6: OrderItem snapshot remains correct
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, times(1)).save(itemCaptor.capture());
        OrderItem capturedItem = itemCaptor.getValue();
        assertEquals("Cotton T-Shirt", capturedItem.getProductName());
        assertEquals(new BigDecimal("15000.00"), capturedItem.getUnitPrice());
        assertEquals(2, capturedItem.getQty());
        assertEquals(new BigDecimal("30000.00"), capturedItem.getSubtotal());
    }

    @Test
    @DisplayName("Phase 2 - Test 3, 4: BUY_NOW order has cart = null and leaves active Cart untouched")
    void createOrder_BuyNowCheckout_OrderCartIsNullAndActiveCartUntouched() {
        BuyNowItemRequest buyNowItem = new BuyNowItemRequest();
        buyNowItem.setVariantId(200L);
        buyNowItem.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCheckoutType("BUY_NOW");
        request.setBuyNowItem(buyNowItem);
        request.setSavedAddressId(50L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(variantRepository.findById(200L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findByVariantVariantId(200L)).thenReturn(Optional.of(inventory));
        when(customerAddressRepository.findByAddressIdAndCustomerCustomerId(50L, 1L)).thenReturn(Optional.of(savedAddress));
        when(shippingService.getShippingQuote(1L, 50L)).thenReturn(shippingQuote);
        when(optionValueRepository.findByVariantVariantId(200L)).thenReturn(Collections.emptyList());

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setOrderId(2001L);
            return o;
        });

        OrderResponse response = checkoutService.createOrder(1L, request);

        assertNotNull(response);
        assertEquals(2001L, response.getOrderId());
        assertEquals("BUY_NOW", response.getCheckoutType());

        // Test 3: BUY_NOW order has cart = null
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertNull(capturedOrder.getCart(), "BUY_NOW order cart must be null");

        // Test 4: BUY_NOW does not query or save active Cart
        verify(cartRepository, never()).findByCustomerCustomerIdAndStatus(anyLong(), anyString());
        verify(cartRepository, never()).save(any(Cart.class));
        assertEquals("ACTIVE", cart.getStatus(), "Active cart must remain untouched");
    }

    @Test
    @DisplayName("Phase 2 - Test 5: Checkout failure rolls back Cart status (Cart A remains ACTIVE and is never saved)")
    void createOrder_CheckoutFailure_RollsBackCartStatus() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSavedAddressId(50L);

        // Simulate inventory failure
        inventory.setQuantity(0);
        when(reservationService.getAvailableStock(200L)).thenReturn(0);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartId(10L)).thenReturn(List.of(cartItem));
        when(inventoryRepository.findByVariantVariantId(200L)).thenReturn(Optional.of(inventory));

        assertThrows(InvalidRequestException.class, () -> checkoutService.createOrder(1L, request));

        // Test 5: Cart status remains ACTIVE and save is never called
        assertEquals("ACTIVE", cart.getStatus(), "Cart A status must remain ACTIVE upon failure");
        verify(cartRepository, never()).save(any(Cart.class));
        verify(orderRepository, never()).save(any(Order.class));
    }
}
