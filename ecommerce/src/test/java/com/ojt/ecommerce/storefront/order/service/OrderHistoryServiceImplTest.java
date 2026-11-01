package com.ojt.ecommerce.storefront.order.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderAddressRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderItemRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.order.dto.OrderDetailResponse;
import com.ojt.ecommerce.storefront.order.dto.OrderListResponse;
import com.ojt.ecommerce.storefront.order.dto.ShipmentTracking;
import com.ojt.ecommerce.storefront.order.port.ShipmentTrackingPort;
import com.ojt.ecommerce.storefront.payment.repository.G5PaymentRepository;
import com.ojt.ecommerce.storefront.shipping.repository.G5ShipmentRepository;
import com.ojt.ecommerce.enums.CourierName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderHistoryServiceImplTest {

    @Mock
    private G5OrderRepository orderRepository;

    @Mock
    private G5OrderItemRepository orderItemRepository;

    @Mock
    private G5OrderAddressRepository orderAddressRepository;

    @Mock
    private G5OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private G5PaymentRepository paymentRepository;

    @Mock
    private G5ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentTrackingPort shipmentTrackingPort;

    @InjectMocks
    private OrderHistoryServiceImpl orderHistoryService;

    private Customer customer;
    private Order order;
    private OrderItem orderItem;
    private OrderAddress orderAddress;
    private Payment payment;
    private OrderStatusHistory statusHistory;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerId(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .build();

        order = Order.builder()
                .orderId(10L)
                .orderNo("ORD-1001")
                .customer(customer)
                .orderStatus("DELIVERED")
                .paymentStatus("SUCCESS")
                .subtotalAmount(new BigDecimal("100000.00"))
                .shippingFee(new BigDecimal("3000.00"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("103000.00"))
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        orderItem = OrderItem.builder()
                .orderItemId(101L)
                .order(order)
                .productName("Wireless Mouse")
                .variantAttributes("{\"Color\":\"Black\"}")
                .qty(2)
                .unitPrice(new BigDecimal("50000.00"))
                .subtotal(new BigDecimal("100000.00"))
                .discountAmount(BigDecimal.ZERO)
                .build();

        orderAddress = OrderAddress.builder()
                .orderAddressId(501L)
                .order(order)
                .recipientName("John Doe")
                .phoneNumber("09123456789")
                .addressLine1("No. 12, Main Street")
                .township("Kyauktada")
                .city("Yangon")
                .regionOrState("Yangon Region")
                .build();

        payment = Payment.builder()
                .paymentId(301L)
                .order(order)
                .paymentMethod("KBZ_PAY")
                .paymentStatus("SUCCESS")
                .transactionRef("TXN-123456")
                .amount(new BigDecimal("103000.00"))
                .paidAt(LocalDateTime.now().minusDays(2))
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        statusHistory = OrderStatusHistory.builder()
                .historyId(401L)
                .order(order)
                .oldStatus("SHIPPED")
                .newStatus("DELIVERED")
                .remark("Package delivered successfully")
                .changedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // =========================================================================
    // 1. getMyOrders Tests (JPA Specification + Batch Fetching)
    // =========================================================================

    @Test
    @DisplayName("Should return paged orders sorted by createdAt DESC with batch-fetched items and shipments")
    void getMyOrders_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);
        when(orderItemRepository.findByOrderOrderIdIn(List.of(10L))).thenReturn(List.of(orderItem));

        Shipment shipment = Shipment.builder()
                .shipmentId(201L)
                .order(order)
                .trackingNumber("TRACK-999")
                .build();
        when(shipmentRepository.findByOrderOrderIdIn(List.of(10L))).thenReturn(List.of(shipment));

        Page<OrderListResponse> result = orderHistoryService.getMyOrders(1L, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        OrderListResponse itemResponse = result.getContent().get(0);
        assertEquals("ORD-1001", itemResponse.getOrderNo());
        assertEquals(new BigDecimal("103000.00"), itemResponse.getTotalAmount());
        assertEquals(1, itemResponse.getItems().size());
        assertEquals("Wireless Mouse", itemResponse.getItems().get(0).getProductName());
        assertEquals(1, itemResponse.getTrackingNumbers().size());
        assertEquals("TRACK-999", itemResponse.getTrackingNumbers().get(0));

        // Verify that sorting defaults to createdAt DESC
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable capturedPageable = pageableCaptor.getValue();
        assertNotNull(capturedPageable.getSort().getOrderFor("createdAt"));
        assertTrue(capturedPageable.getSort().getOrderFor("createdAt").isDescending());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when startDate is after endDate")
    void getMyOrders_ThrowsException_WhenStartDateAfterEndDate() {
        Pageable pageable = PageRequest.of(0, 10);

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> orderHistoryService.getMyOrders(1L, "2026-10-20", "2026-10-10", null, null, pageable));

        assertEquals("startDate cannot be after endDate", ex.getMessage());
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Should return empty page when no orders match criteria")
    void getMyOrders_EmptyResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        Page<OrderListResponse> result = orderHistoryService.getMyOrders(1L, null, null, "CANCELLED", null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verifyNoInteractions(orderItemRepository);
        verifyNoInteractions(shipmentRepository);
    }

    // =========================================================================
    // 2. getOrderById Tests (IDOR Protection & Detail Mapping)
    // =========================================================================

    @Test
    @DisplayName("Should return order detail successfully when customer owns the order")
    void getOrderById_Success() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderOrderId(10L)).thenReturn(List.of(orderItem));
        when(orderAddressRepository.findByOrderOrderId(10L)).thenReturn(Optional.of(orderAddress));
        when(paymentRepository.findFirstByOrderOrderIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(payment));
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByChangedAtAsc(10L)).thenReturn(List.of(statusHistory));

        ShipmentTracking tracking = ShipmentTracking.builder()
                .courierName(CourierName.NINJAVAN)
                .trackingNumber("TRACK-999")
                .shipmentStatus("DELIVERED")
                .build();
        when(shipmentTrackingPort.getTrackingByOrderId(10L)).thenReturn(List.of(tracking));

        OrderDetailResponse response = orderHistoryService.getOrderById(1L, 10L);

        assertNotNull(response);
        assertEquals(10L, response.getOrderId());
        assertEquals("ORD-1001", response.getOrderNo());
        assertEquals("DELIVERED", response.getOrderStatus());
        assertEquals(new BigDecimal("103000.00"), response.getTotalAmount());

        // Address assertions
        assertNotNull(response.getShippingAddress());
        assertEquals("John Doe", response.getShippingAddress().getRecipientName());
        assertEquals("Kyauktada", response.getShippingAddress().getTownship());

        // Payment assertions
        assertNotNull(response.getPayment());
        assertEquals("KBZ_PAY", response.getPayment().getPaymentMethod());

        // Items assertions
        assertEquals(1, response.getItems().size());
        assertEquals("Wireless Mouse", response.getItems().get(0).getProductName());

        // Tracking assertions
        assertEquals(1, response.getShipments().size());
        assertEquals(CourierName.NINJAVAN, response.getShipments().get(0).getCourierName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException (IDOR guard) when order belongs to different customer")
    void getOrderById_ThrowsNotFound_WhenIdorMismatch() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        // Authenticated customer is 99L, but order belongs to customer 1L
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderHistoryService.getOrderById(99L, 10L));

        assertEquals("Order not found", ex.getMessage());
        verifyNoInteractions(orderItemRepository);
        verifyNoInteractions(orderAddressRepository);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when order does not exist")
    void getOrderById_ThrowsNotFound_WhenOrderDoesNotExist() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderHistoryService.getOrderById(1L, 999L));

        assertEquals("Order not found", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when order address is missing")
    void getOrderById_ThrowsNotFound_WhenAddressMissing() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderOrderId(10L)).thenReturn(List.of(orderItem));
        when(orderAddressRepository.findByOrderOrderId(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderHistoryService.getOrderById(1L, 10L));

        assertEquals("Order address missing", ex.getMessage());
    }
}
