package com.ojt.ecommerce.storefront.refund.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderItemRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.refund.dto.RefundEligibilityDto;
import com.ojt.ecommerce.storefront.refund.dto.RefundRequestDto;
import com.ojt.ecommerce.storefront.refund.dto.RefundResponseDto;
import com.ojt.ecommerce.storefront.refund.repository.RefundRequestRepository;
import com.ojt.ecommerce.storefront.shipping.repository.G5ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private G5OrderItemRepository orderItemRepository;

    @Mock
    private G5ShipmentRepository shipmentRepository;

    @Mock
    private G5OrderStatusHistoryRepository orderStatusHistoryRepository;

    @InjectMocks
    private RefundServiceImpl refundService;

    private Customer customer;
    private Order order;
    private OrderItem orderItem;

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
                .paymentStatus("SUCCESS")
                .orderStatus("DELIVERED")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        orderItem = OrderItem.builder()
                .orderItemId(100L)
                .order(order)
                .productName("Wireless Headphones")
                .variantAttributes("{\"Color\":\"Black\"}")
                .qty(3)
                .unitPrice(new BigDecimal("50000.00"))
                .subtotal(new BigDecimal("150000.00"))
                .build();
    }

    // =========================================================================
    // 1. createRefundRequest Tests
    // =========================================================================

    @Test
    @DisplayName("Should successfully create refund request when order is delivered within 7 days")
    void createRefundRequest_Success_WhenDeliveredWithin7Days() {
        // Arrange: Delivered 2 days ago via shipment
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(2))
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));
        when(refundRequestRepository.getReservedQuantity(100L)).thenReturn(0);

        RefundRequest savedRequest = RefundRequest.builder()
                .refundId(1L)
                .orderItem(orderItem)
                .refundQuantity(1)
                .requestReason("DAMAGED_ITEM")
                .refundAmount(new BigDecimal("50000.00"))
                .status("PENDING")
                .requestedAt(LocalDateTime.now())
                .build();

        when(refundRequestRepository.save(any(RefundRequest.class))).thenReturn(savedRequest);

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(100L);
        dto.setRefundQuantity(1);
        dto.setRequestReason("DAMAGED_ITEM");

        // Act
        RefundResponseDto response = refundService.createRefundRequest(1L, dto);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getRefundId());
        assertEquals(100L, response.getOrderItemId());
        assertEquals("PENDING", response.getStatus());
        assertEquals("DAMAGED_ITEM", response.getRequestReason());
        assertEquals(new BigDecimal("50000.00"), response.getRefundAmount());
        assertEquals(2, response.getRemainingRefundableQuantity()); // 3 ordered - 1 requested = 2 remaining
        verify(refundRequestRepository, times(1)).save(any(RefundRequest.class));
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when request reason is invalid")
    void createRefundRequest_ThrowsException_WhenInvalidReason() {
        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(100L);
        dto.setRefundQuantity(1);
        dto.setRequestReason("INVALID_REASON");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> refundService.createRefundRequest(1L, dto));
        assertEquals("Invalid refund reason", ex.getMessage());
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when refund quantity is 0 or negative")
    void createRefundRequest_ThrowsException_WhenQuantityNotPositive() {
        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(100L);
        dto.setRefundQuantity(0);
        dto.setRequestReason("DAMAGED_ITEM");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> refundService.createRefundRequest(1L, dto));
        assertEquals("Refund quantity must be positive", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when orderItem does not exist")
    void createRefundRequest_ThrowsException_WhenOrderItemNotFound() {
        when(orderItemRepository.findById(999L)).thenReturn(Optional.empty());

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(999L);
        dto.setRefundQuantity(1);
        dto.setRequestReason("DAMAGED_ITEM");

        assertThrows(ResourceNotFoundException.class,
                () -> refundService.createRefundRequest(1L, dto));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException (IDOR guard) when order belongs to different customer")
    void createRefundRequest_ThrowsException_WhenIdorMismatch() {
        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(100L);
        dto.setRefundQuantity(1);
        dto.setRequestReason("DAMAGED_ITEM");

        // Authenticated customer is 99L, but order belongs to customer 1L
        assertThrows(ResourceNotFoundException.class,
                () -> refundService.createRefundRequest(99L, dto));
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when order payment status is not SUCCESS")
    void createRefundRequest_ThrowsException_WhenOrderNotPaid() {
        order.setPaymentStatus("PENDING");
        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(100L);
        dto.setRefundQuantity(1);
        dto.setRequestReason("DAMAGED_ITEM");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> refundService.createRefundRequest(1L, dto));
        assertTrue(ex.getMessage().contains("must be PAID"));
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when order is NOT delivered yet")
    void createRefundRequest_ThrowsException_WhenOrderNotDelivered() {
        order.setOrderStatus("PROCESSING");
        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(Collections.emptyList());

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(100L);
        dto.setRefundQuantity(1);
        dto.setRequestReason("DAMAGED_ITEM");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> refundService.createRefundRequest(1L, dto));
        assertEquals("Refund can only be requested after the order has been delivered.", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when delivered more than 7 days ago (Expired)")
    void createRefundRequest_ThrowsException_WhenExpiredAfter7Days() {
        // Delivered 8 days ago
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(8))
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(100L);
        dto.setRefundQuantity(1);
        dto.setRequestReason("DEFECTIVE_PRODUCT");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> refundService.createRefundRequest(1L, dto));
        assertTrue(ex.getMessage().contains("Refund request period has expired"));
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when requested quantity exceeds remaining quantity")
    void createRefundRequest_ThrowsException_WhenQuantityExceedsRemaining() {
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(1))
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));
        when(refundRequestRepository.getReservedQuantity(100L)).thenReturn(2); // ordered 3, already reserved 2 -> remaining 1

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(100L);
        dto.setRefundQuantity(2); // requesting 2, but only 1 remaining!
        dto.setRequestReason("WRONG_ITEM_SENT");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> refundService.createRefundRequest(1L, dto));
        assertTrue(ex.getMessage().contains("Requested quantity exceeds remaining refundable quantity"));
    }

    // =========================================================================
    // 2. getRefundEligibility Tests
    // =========================================================================

    @Test
    @DisplayName("Should return eligible with daysRemaining when delivered 3 days ago")
    void getRefundEligibility_Eligible_Within7Days() {
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(3))
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(refundRequestRepository.getReservedQuantity(100L)).thenReturn(1);
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));

        RefundEligibilityDto eligibility = refundService.getRefundEligibility(1L, 100L);

        assertNotNull(eligibility);
        assertTrue(eligibility.isCanRequestRefund());
        assertTrue(eligibility.getCanRequestRefund());
        assertTrue(refundService.isCanRequestRefund(1L, 100L));
        assertNull(eligibility.getIneligibleReason());
        assertEquals(3, eligibility.getOrderedQuantity());
        assertEquals(1, eligibility.getAlreadyReservedRefundQuantity());
        assertEquals(2, eligibility.getRemainingRefundableQuantity());
        assertNotNull(eligibility.getDaysRemaining());
        assertTrue(eligibility.getDaysRemaining() >= 3 && eligibility.getDaysRemaining() <= 5);
    }

    @Test
    @DisplayName("Should return ineligible when order has not been delivered")
    void getRefundEligibility_Ineligible_WhenNotDelivered() {
        order.setOrderStatus("SHIPPED");
        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(Collections.emptyList());

        RefundEligibilityDto eligibility = refundService.getRefundEligibility(1L, 100L);

        assertNotNull(eligibility);
        assertFalse(eligibility.isCanRequestRefund());
        assertFalse(eligibility.getCanRequestRefund());
        assertFalse(refundService.isCanRequestRefund(1L, 100L));
        assertEquals("Refund can only be requested after the order has been delivered.", eligibility.getIneligibleReason());
    }

    @Test
    @DisplayName("Should return ineligible with daysRemaining 0 when 7 days have passed")
    void getRefundEligibility_Ineligible_WhenExpiredAfter7Days() {
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(10))
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));

        RefundEligibilityDto eligibility = refundService.getRefundEligibility(1L, 100L);

        assertNotNull(eligibility);
        assertFalse(eligibility.isCanRequestRefund());
        assertFalse(eligibility.getCanRequestRefund());
        assertFalse(refundService.isCanRequestRefund(1L, 100L));
        assertTrue(eligibility.getIneligibleReason().contains("Refund window expired"));
        assertEquals(0, eligibility.getDaysRemaining());
    }

    @Test
    @DisplayName("Should return ineligible when remaining refundable quantity is 0")
    void getRefundEligibility_Ineligible_WhenRemainingQtyZero() {
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(1))
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(refundRequestRepository.getReservedQuantity(100L)).thenReturn(3); // All 3 reserved
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));

        RefundEligibilityDto eligibility = refundService.getRefundEligibility(1L, 100L);

        assertNotNull(eligibility);
        assertFalse(eligibility.isCanRequestRefund());
        assertFalse(eligibility.getCanRequestRefund());
        assertFalse(refundService.isCanRequestRefund(1L, 100L));
        assertTrue(eligibility.getIneligibleReason().contains("All items have already been refunded"));
    }

    @Test
    @DisplayName("Should return refundStatus PENDING when a pending refund request exists")
    void getRefundEligibility_WithPendingStatus() {
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(2))
                .build();

        RefundRequest req = RefundRequest.builder()
                .refundId(1L)
                .orderItem(orderItem)
                .status("PENDING")
                .refundQuantity(1)
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(refundRequestRepository.getReservedQuantity(100L)).thenReturn(1);
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));
        when(refundRequestRepository.findFirstByOrderItemOrderItemIdOrderByRequestedAtDesc(100L)).thenReturn(Optional.of(req));

        RefundEligibilityDto eligibility = refundService.getRefundEligibility(1L, 100L);

        assertNotNull(eligibility);
        assertEquals("PENDING", eligibility.getRefundStatus());
    }

    @Test
    @DisplayName("Should return refundStatus APPROVED when refund was approved by backoffice")
    void getRefundEligibility_WithApprovedStatus() {
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(2))
                .build();

        RefundRequest req = RefundRequest.builder()
                .refundId(1L)
                .orderItem(orderItem)
                .status("APPROVED")
                .refundQuantity(1)
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(refundRequestRepository.getReservedQuantity(100L)).thenReturn(1);
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));
        when(refundRequestRepository.findFirstByOrderItemOrderItemIdOrderByRequestedAtDesc(100L)).thenReturn(Optional.of(req));

        RefundEligibilityDto eligibility = refundService.getRefundEligibility(1L, 100L);

        assertNotNull(eligibility);
        assertEquals("APPROVED", eligibility.getRefundStatus());
    }

    // =========================================================================
    // 3. getMyRefunds & getRefundById Tests
    // =========================================================================

    @Test
    @DisplayName("Should return customer refund requests successfully")
    void getMyRefunds_Success() {
        RefundRequest req = RefundRequest.builder()
                .refundId(1L)
                .orderItem(orderItem)
                .refundQuantity(1)
                .requestReason("DEFECTIVE_PRODUCT")
                .refundAmount(new BigDecimal("50000.00"))
                .status("PENDING")
                .requestedAt(LocalDateTime.now())
                .build();

        when(refundRequestRepository.findByOrderItemOrderCustomerCustomerIdOrderByRequestedAtDesc(1L))
                .thenReturn(List.of(req));
        when(refundRequestRepository.getReservedQuantity(100L)).thenReturn(1);

        List<RefundResponseDto> result = refundService.getMyRefunds(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DEFECTIVE_PRODUCT", result.get(0).getRequestReason());
    }

    @Test
    @DisplayName("Should get refund by ID successfully")
    void getRefundById_Success() {
        RefundRequest req = RefundRequest.builder()
                .refundId(5L)
                .orderItem(orderItem)
                .refundQuantity(2)
                .requestReason("WRONG_ITEM_SENT")
                .refundAmount(new BigDecimal("100000.00"))
                .status("APPROVED")
                .requestedAt(LocalDateTime.now())
                .build();

        when(refundRequestRepository.findById(5L)).thenReturn(Optional.of(req));
        when(refundRequestRepository.getReservedQuantity(100L)).thenReturn(2);

        RefundResponseDto response = refundService.getRefundById(1L, 5L);

        assertNotNull(response);
        assertEquals(5L, response.getRefundId());
        assertEquals("APPROVED", response.getStatus());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException (IDOR guard) when accessing another customer's refund")
    void getRefundById_ThrowsIdorMismatch() {
        RefundRequest req = RefundRequest.builder()
                .refundId(5L)
                .orderItem(orderItem) // belongs to customer 1L
                .build();

        when(refundRequestRepository.findById(5L)).thenReturn(Optional.of(req));

        assertThrows(ResourceNotFoundException.class,
                () -> refundService.getRefundById(99L, 5L));
    }

    @Test
    @DisplayName("Should allow creating refund request when order is PARTIALLY_REFUNDED")
    void createRefundRequest_Success_WhenOrderIsPartiallyRefunded() {
        order.setPaymentStatus("PARTIALLY_REFUNDED");
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(1))
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));
        when(refundRequestRepository.getReservedQuantity(100L)).thenReturn(1); // 3 ordered, 1 reserved -> 2 remaining

        RefundRequest saved = RefundRequest.builder()
                .refundId(2L)
                .orderItem(orderItem)
                .refundQuantity(1)
                .requestReason("DEFECTIVE_PRODUCT")
                .refundAmount(new BigDecimal("50000.00"))
                .status("PENDING")
                .requestedAt(LocalDateTime.now())
                .build();
        when(refundRequestRepository.save(any(RefundRequest.class))).thenReturn(saved);

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(100L);
        dto.setRefundQuantity(1);
        dto.setRequestReason("DEFECTIVE_PRODUCT");

        RefundResponseDto res = refundService.createRefundRequest(1L, dto);
        assertNotNull(res);
        assertEquals("PENDING", res.getStatus());
        assertEquals(1, res.getRemainingRefundableQuantity());
    }

    @Test
    @DisplayName("Should return approved ineligible reason when refund was approved and remaining qty is 0")
    void getRefundEligibility_WhenApprovedAndFullyRefunded() {
        order.setPaymentStatus("REFUNDED");
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(2))
                .build();

        RefundRequest req = RefundRequest.builder()
                .refundId(1L)
                .orderItem(orderItem)
                .status("APPROVED")
                .refundQuantity(3)
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(refundRequestRepository.getReservedQuantity(100L)).thenReturn(3); // ordered 3, all 3 reserved
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));
        when(refundRequestRepository.findFirstByOrderItemOrderItemIdOrderByRequestedAtDesc(100L)).thenReturn(Optional.of(req));

        RefundEligibilityDto eligibility = refundService.getRefundEligibility(1L, 100L);

        assertNotNull(eligibility);
        assertFalse(eligibility.isCanRequestRefund());
        assertEquals("APPROVED", eligibility.getRefundStatus());
        assertTrue(eligibility.getIneligibleReason().contains("approved"));
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when refund was rejected by inventory")
    void createRefundRequest_ThrowsException_WhenRefundWasRejected() {
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(1))
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));
        when(refundRequestRepository.existsByOrderItemOrderItemIdAndStatus(100L, "REJECTED")).thenReturn(true);

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(100L);
        dto.setRefundQuantity(1);
        dto.setRequestReason("DEFECTIVE_PRODUCT");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> refundService.createRefundRequest(1L, dto));
        assertTrue(ex.getMessage().contains("rejected and cannot be requested again"));
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when a refund is already pending review")
    void createRefundRequest_ThrowsException_WhenRefundIsAlreadyPending() {
        Shipment shipment = Shipment.builder()
                .shipmentStatus("DELIVERED")
                .deliveredAt(LocalDateTime.now().minusDays(1))
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(shipmentRepository.findByOrderOrderId(10L)).thenReturn(List.of(shipment));
        when(refundRequestRepository.existsByOrderItemOrderItemIdAndStatus(100L, "PENDING")).thenReturn(true);

        RefundRequestDto dto = new RefundRequestDto();
        dto.setOrderItemId(100L);
        dto.setRefundQuantity(1);
        dto.setRequestReason("DEFECTIVE_PRODUCT");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> refundService.createRefundRequest(1L, dto));
        assertTrue(ex.getMessage().contains("already pending review"));
    }

    @Test
    @DisplayName("Should return ineligible with REJECTED status when refund request was rejected")
    void getRefundEligibility_WhenRejected_ShouldBeIneligible() {
        RefundRequest req = RefundRequest.builder()
                .refundId(1L)
                .orderItem(orderItem)
                .status("REJECTED")
                .refundQuantity(1)
                .remark("Product was damaged after delivery by customer")
                .build();

        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(orderItem));
        when(refundRequestRepository.getReservedQuantity(100L)).thenReturn(1);
        when(refundRequestRepository.findFirstByOrderItemOrderItemIdOrderByRequestedAtDesc(100L)).thenReturn(Optional.of(req));

        RefundEligibilityDto eligibility = refundService.getRefundEligibility(1L, 100L);

        assertNotNull(eligibility);
        assertFalse(eligibility.isCanRequestRefund());
        assertEquals("REJECTED", eligibility.getRefundStatus());
        assertEquals("Product was damaged after delivery by customer", eligibility.getRemark());
        assertEquals(0, eligibility.getRemainingRefundableQuantity());
        assertTrue(eligibility.getIneligibleReason().contains("rejected"));
    }
}
