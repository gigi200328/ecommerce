package com.ojt.ecommerce.storefront.payment.service;

import com.ojt.ecommerce.backofficeinventory.service.AdminNotificationService;
import com.ojt.ecommerce.backofficeinventory.service.InventoryService;
import com.ojt.ecommerce.entity.Cart;
import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.entity.Payment;
import com.ojt.ecommerce.entity.PaymentCallback;
import com.ojt.ecommerce.storefront.cart.dto.RestoreToCartResponse;
import com.ojt.ecommerce.storefront.cart.repository.CartRepository;
import com.ojt.ecommerce.storefront.cart.service.CartService;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.exception.AccessDeniedException;
import com.ojt.ecommerce.storefront.exception.ConflictException;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.payment.dto.PaymentCancelResponse;
import com.ojt.ecommerce.storefront.payment.dto.MmspgInitiateResponse;
import com.ojt.ecommerce.storefront.payment.dto.MockCallbackRequest;
import com.ojt.ecommerce.storefront.payment.dto.MockCallbackResponse;
import com.ojt.ecommerce.storefront.payment.dto.PaymentDetailResponse;
import com.ojt.ecommerce.storefront.payment.dto.PaymentInitiateRequest;
import com.ojt.ecommerce.storefront.payment.dto.PaymentInitiateResponse;
import com.ojt.ecommerce.storefront.payment.event.OrderPaidEvent;
import com.ojt.ecommerce.storefront.payment.repository.G5PaymentRepository;
import com.ojt.ecommerce.storefront.payment.repository.PaymentCallbackRepository;
import com.ojt.ecommerce.storefront.reservation.service.InventoryReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private G5PaymentRepository paymentRepository;

    @Mock
    private PaymentCallbackRepository paymentCallbackRepository;

    @Mock
    private G5OrderRepository orderRepository;

    @Mock
    private G5OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartService cartService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InventoryReservationService reservationService;

    @Mock
    private AdminNotificationService notificationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Customer mockCustomer;
    private Order mockOrder;
    private Payment mockPayment;
    private Cart mockCart;

    @BeforeEach
    void setUp() {
        mockCustomer = Customer.builder()
                .customerId(1L)
                .email("customer@example.com")
                .fullName("Test Customer")
                .build();

        mockOrder = Order.builder()
                .orderId(100L)
                .orderNo("ORD-TEST-001")
                .customer(mockCustomer)
                .totalAmount(BigDecimal.valueOf(25000))
                .orderStatus("PENDING")
                .paymentStatus("PENDING")
                .checkoutType("CART")
                .createdAt(LocalDateTime.now())
                .build();

        mockPayment = Payment.builder()
                .paymentId(10L)
                .order(mockOrder)
                .transactionRef("TXN-TEST-001")
                .paymentMethod("MINI_BANKING")
                .paymentStatus("PENDING")
                .amount(BigDecimal.valueOf(25000))
                .createdAt(LocalDateTime.now())
                .build();

        mockCart = Cart.builder()
                .cartId(50L)
                .customer(mockCustomer)
                .status("ACTIVE")
                .version(0L)
                .createdAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(paymentService, "pgBaseUrl", "http://localhost:8004/api/v1");
        ReflectionTestUtils.setField(paymentService, "pgClientId", "test-client");
        ReflectionTestUtils.setField(paymentService, "pgBranchId", "test-branch");
        ReflectionTestUtils.setField(paymentService, "pgClientSecret", "test-secret");
        paymentService.setRestTemplate(restTemplate);
    }

    // =========================================================================
    // initiatePayment Tests
    // =========================================================================

    @Test
    @DisplayName("initiatePayment: Order not found throws ResourceNotFoundException")
    void initiatePayment_WhenOrderNotFound_ShouldThrowResourceNotFoundException() {
        when(orderRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        PaymentInitiateRequest request = new PaymentInitiateRequest();
        request.setPaymentMethod("MINI_BANKING");

        assertThatThrownBy(() -> paymentService.initiatePayment(1L, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 999");

        verifyNoInteractions(restTemplate, paymentRepository);
    }

    @Test
    @DisplayName("initiatePayment: Order customer mismatch throws AccessDeniedException")
    void initiatePayment_WhenCustomerMismatch_ShouldThrowAccessDeniedException() {
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        PaymentInitiateRequest request = new PaymentInitiateRequest();
        request.setPaymentMethod("MINI_BANKING");

        assertThatThrownBy(() -> paymentService.initiatePayment(999L, 100L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Order belongs to a different customer");

        verifyNoInteractions(restTemplate, paymentRepository);
    }

    @Test
    @DisplayName("initiatePayment: Already-paid order throws InvalidRequestException")
    void initiatePayment_WhenOrderAlreadyPaid_ShouldThrowInvalidRequestException() {
        mockOrder.setOrderStatus("PAID");
        mockOrder.setPaymentStatus("SUCCESS");
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        PaymentInitiateRequest request = new PaymentInitiateRequest();
        request.setPaymentMethod("MINI_BANKING");

        assertThatThrownBy(() -> paymentService.initiatePayment(1L, 100L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Order is already paid");

        verifyNoInteractions(restTemplate, paymentRepository);
    }

    @Test
    @DisplayName("initiatePayment: Order with non-payable status throws InvalidRequestException")
    void initiatePayment_WhenOrderNotPayable_ShouldThrowInvalidRequestException() {
        mockOrder.setOrderStatus("CANCELLED");
        mockOrder.setPaymentStatus("CANCELLED");
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        PaymentInitiateRequest request = new PaymentInitiateRequest();
        request.setPaymentMethod("MINI_BANKING");

        assertThatThrownBy(() -> paymentService.initiatePayment(1L, 100L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Order is not payable");

        verifyNoInteractions(restTemplate, paymentRepository);
    }

    @Test
    @DisplayName("initiatePayment: Valid unpaid order acquires lock, calls gateway, and saves payment")
    void initiatePayment_WhenValidUnpaidOrder_ShouldObtainLockAndInitiatePayment() {
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        MmspgInitiateResponse mockGatewayResponse = new MmspgInitiateResponse();
        mockGatewayResponse.setTransactionReference("TXN-NEW-002");
        mockGatewayResponse.setPaymentUrl("http://localhost:8004/checkout?token=abc");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(MmspgInitiateResponse.class)))
                .thenReturn(new ResponseEntity<>(mockGatewayResponse, HttpStatus.OK));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setPaymentId(20L);
            return saved;
        });

        PaymentInitiateRequest request = new PaymentInitiateRequest();
        request.setPaymentMethod("MINI_BANKING");

        PaymentInitiateResponse response = paymentService.initiatePayment(1L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(20L);
        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(25000));
        assertThat(response.getRedirectUrl()).isEqualTo("http://localhost:8004/checkout?token=abc");

        verify(orderRepository).findByIdWithLock(100L);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("initiatePayment: Reuses existing Payment row when MMSPG returns the same transaction reference")
    void initiatePayment_WhenSameTransactionRef_ShouldReuseExistingPaymentRow() {
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        MmspgInitiateResponse mockGatewayResponse = new MmspgInitiateResponse();
        mockGatewayResponse.setTransactionReference("TXN-EXISTING-001");
        mockGatewayResponse.setPaymentUrl("http://localhost:8004/checkout?token=xyz");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(MmspgInitiateResponse.class)))
                .thenReturn(new ResponseEntity<>(mockGatewayResponse, HttpStatus.OK));

        Payment existingPayment = new Payment();
        existingPayment.setPaymentId(50L);
        existingPayment.setOrder(mockOrder);
        existingPayment.setTransactionRef("TXN-EXISTING-001");
        existingPayment.setPaymentStatus("PENDING");

        when(paymentRepository.findByTransactionRef("TXN-EXISTING-001")).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentInitiateRequest request = new PaymentInitiateRequest();
        request.setPaymentMethod("MINI_BANKING");

        PaymentInitiateResponse response = paymentService.initiatePayment(1L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(50L);
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");

        // Verify save was called on existing payment object
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getPaymentId()).isEqualTo(50L);
        assertThat(paymentCaptor.getValue().getTransactionRef()).isEqualTo("TXN-EXISTING-001");
    }

    // =========================================================================
    // getLatestPayment Tests
    // =========================================================================

    @Test
    @DisplayName("getLatestPayment: Order not found throws ResourceNotFoundException")
    void getLatestPayment_WhenOrderNotFound_ShouldThrowResourceNotFoundException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getLatestPayment(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found");

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("getLatestPayment: Order customer mismatch throws AccessDeniedException")
    void getLatestPayment_WhenCustomerMismatch_ShouldThrowAccessDeniedException() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> paymentService.getLatestPayment(999L, 100L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Order belongs to a different customer");

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("getLatestPayment: Payment found returns PaymentDetailResponse")
    void getLatestPayment_WhenPaymentFound_ShouldReturnPaymentDetail() {
        mockPayment.setPaidAt(LocalDateTime.of(2026, 11, 3, 12, 0, 0));
        when(orderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.findFirstByOrderOrderIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(mockPayment));

        PaymentDetailResponse response = paymentService.getLatestPayment(1L, 100L);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(10L);
        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(response.getPaymentMethod()).isEqualTo("MINI_BANKING");
        assertThat(response.getTransactionRef()).isEqualTo("TXN-TEST-001");
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(25000));
        assertThat(response.getPaidAt()).isEqualTo(mockPayment.getPaidAt().toString());
    }

    @Test
    @DisplayName("getLatestPayment: No payment found returns null")
    void getLatestPayment_WhenNoPaymentFound_ShouldReturnNull() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.findFirstByOrderOrderIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.empty());

        PaymentDetailResponse response = paymentService.getLatestPayment(1L, 100L);

        assertThat(response).isNull();
    }

    // =========================================================================
    // processMockCallback Tests
    // =========================================================================

    @Test
    @DisplayName("processMockCallback: TEST_PING request returns SUCCESS without looking up payment")
    void processMockCallback_WhenTestPingRequest_ShouldReturnSuccess() {
        MockCallbackRequest request = new MockCallbackRequest();
        request.setEvent("TEST_PING");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentStatus()).isEqualTo("SUCCESS");
        verifyNoInteractions(paymentRepository, orderRepository);
    }

    @Test
    @DisplayName("processMockCallback: TEST_PING with raw payload and valid signature returns SUCCESS")
    void processMockCallback_WhenTestPingWithSignature_ShouldReturnSuccess() {
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "pgWebhookSecret", "test-secret-123");
        String rawPayload = "{\"event\":\"TEST_PING\",\"timestamp\":\"2026-11-09T15:00:00\"}";
        String signature = com.ojt.ecommerce.common.util.HmacSignatureUtil.generateSignature(rawPayload, "test-secret-123");

        MockCallbackResponse response = paymentService.processMockCallback(rawPayload, signature);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentStatus()).isEqualTo("SUCCESS");
        verifyNoInteractions(paymentRepository, orderRepository);
    }

    @Test
    @DisplayName("processMockCallback: Unknown transactionRef throws ResourceNotFoundException")
    void processMockCallback_WhenPaymentNotFound_ShouldThrowResourceNotFoundException() {
        when(paymentRepository.findByTransactionRef("UNKNOWN-TXN")).thenReturn(Optional.empty());

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-ERR-1");
        request.setTransactionRef("UNKNOWN-TXN");
        request.setStatus("SUCCESS");

        assertThatThrownBy(() -> paymentService.processMockCallback(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found");
    }

    @Test
    @DisplayName("processMockCallback: Payment with no order throws InvalidRequestException")
    void processMockCallback_WhenPaymentHasNoOrder_ShouldThrowInvalidRequestException() {
        mockPayment.setOrder(null);
        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-ERR-2");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        assertThatThrownBy(() -> paymentService.processMockCallback(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Payment is not associated with an order");
    }

    @Test
    @DisplayName("processMockCallback: Order not found during lock acquisition throws ResourceNotFoundException")
    void processMockCallback_WhenOrderNotFound_ShouldThrowResourceNotFoundException() {
        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.empty());

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-ERR-3");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        assertThatThrownBy(() -> paymentService.processMockCallback(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 100");
    }

    @Test
    @DisplayName("processMockCallback: Mismatched payment/order association throws InvalidRequestException")
    void processMockCallback_WhenOrderPaymentMismatch_ShouldThrowInvalidRequestException() {
        Order mismatchedOrder = Order.builder()
                .orderId(999L)
                .orderNo("ORD-DIFF-001")
                .customer(mockCustomer)
                .build();

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mismatchedOrder));

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-MISMATCH");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        assertThatThrownBy(() -> paymentService.processMockCallback(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Payment and Order association mismatch");

        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("processMockCallback: Duplicate eventId returns existing payment status idempotently")
    void processMockCallback_WhenDuplicateEventId_ShouldReturnExistingPaymentStatus() {
        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-DUP-1")).thenReturn(true);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-DUP-1");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getPaymentStatus()).isEqualTo("PENDING");

        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("processMockCallback: Payment already recorded as SUCCESS saves callback and returns SUCCESS")
    void processMockCallback_WhenPaymentAlreadySuccess_ShouldRecordCallbackAndReturnSuccess() {
        mockPayment.setPaymentStatus("SUCCESS");
        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-ALREADY-SUCCESS")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-ALREADY-SUCCESS");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getPaymentStatus()).isEqualTo("SUCCESS");

        verify(paymentCallbackRepository).save(any(PaymentCallback.class));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("processMockCallback: SUCCESS callback on CART order updates order, history, checks out linked Cart A, and publishes event")
    void processMockCallback_WhenSuccessStatusAndCartOrder_ShouldUpdateOrderSaveHistoryCheckoutCartAndPublishEvent() {
        mockCart.setStatus("PENDING_PAYMENT");
        mockOrder.setCart(mockCart);
        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-CART-1")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-CART-1");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getPaymentStatus()).isEqualTo("SUCCESS");

        assertThat(mockPayment.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(mockPayment.getPaidAt()).isNotNull();
        verify(paymentRepository).save(mockPayment);

        assertThat(mockOrder.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(mockOrder.getOrderStatus()).isEqualTo("PAID");
        verify(orderRepository).save(mockOrder);

        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        assertThat(mockCart.getStatus()).isEqualTo("CHECKED_OUT");
        verify(cartRepository).save(mockCart);
        verify(cartRepository, never()).findByCustomerCustomerIdAndStatus(anyLong(), anyString());

        verify(eventPublisher).publishEvent(any(OrderPaidEvent.class));
        verify(paymentCallbackRepository).save(any(PaymentCallback.class));
    }

    @Test
    @DisplayName("processMockCallback: SUCCESS callback on BUY_NOW order preserves active cart")
    void processMockCallback_WhenSuccessStatusAndBuyNowOrder_ShouldPreserveCartAndPublishEvent() {
        mockOrder.setCheckoutType("BUY_NOW");
        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-BUYNOW-1")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-BUYNOW-1");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(mockOrder.getOrderStatus()).isEqualTo("PAID");
        assertThat(mockOrder.getPaymentStatus()).isEqualTo("SUCCESS");

        verify(cartRepository, never()).findByCustomerCustomerIdAndStatus(anyLong(), anyString());
        verify(cartRepository, never()).save(any());
        verify(eventPublisher).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    @DisplayName("processMockCallback: Late SUCCESS callback on already-paid order skips duplicate side-effects")
    void processMockCallback_WhenSuccessStatusAndOrderAlreadyPaid_ShouldSkipDuplicateSideEffects() {
        mockOrder.setOrderStatus("PAID");
        mockOrder.setPaymentStatus("SUCCESS");
        mockOrder.setCheckoutType("CART");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-LATE-SUCCESS")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-LATE-SUCCESS");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(mockPayment.getPaymentStatus()).isEqualTo("SUCCESS");
        verify(paymentRepository).save(mockPayment);

        verify(orderRepository, never()).save(mockOrder);
        verify(orderStatusHistoryRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(paymentCallbackRepository).save(any(PaymentCallback.class));
    }

    @Test
    @DisplayName("processMockCallback: FAILED callback on unpaid order marks payment and order as FAILED")
    void processMockCallback_WhenFailedStatusAndOrderUnpaid_ShouldMarkPaymentAndOrderFailed() {
        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-FAILED-1")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-FAILED-1");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("FAILED");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(mockPayment.getPaymentStatus()).isEqualTo("FAILED");
        verify(paymentRepository).save(mockPayment);

        assertThat(mockOrder.getPaymentStatus()).isEqualTo("FAILED");
        verify(orderRepository).save(mockOrder);

        verify(eventPublisher, never()).publishEvent(any());
        verify(paymentCallbackRepository).save(any(PaymentCallback.class));
    }

    @Test
    @DisplayName("processMockCallback: FAILED callback does not downgrade an already SUCCESS payment or order")
    void processMockCallback_WhenFailedStatusAndPaymentAlreadySuccess_ShouldNotDowngradePayment() {
        mockPayment.setPaymentStatus("SUCCESS");
        mockOrder.setOrderStatus("PAID");
        mockOrder.setPaymentStatus("SUCCESS");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-LATE-FAIL")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-LATE-FAIL");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("FAILED");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(mockPayment.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(mockOrder.getOrderStatus()).isEqualTo("PAID");
        assertThat(mockOrder.getPaymentStatus()).isEqualTo("SUCCESS");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("processMockCallback: Delayed FAILED callback after successful payment preserves PAID/SUCCESS order")
    void processMockCallback_WhenDelayedFailedCallbackAfterSuccess_ShouldPreservePaidOrder() {
        mockOrder.setOrderStatus("PAID");
        mockOrder.setPaymentStatus("SUCCESS");
        mockPayment.setPaymentStatus("PENDING");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-DELAYED-FAIL")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-DELAYED-FAIL");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("FAILED");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(mockPayment.getPaymentStatus()).isEqualTo("FAILED");
        verify(paymentRepository).save(mockPayment);

        assertThat(mockOrder.getOrderStatus()).isEqualTo("PAID");
        assertThat(mockOrder.getPaymentStatus()).isEqualTo("SUCCESS");
        verify(orderRepository, never()).save(mockOrder);
    }

    @Test
    @DisplayName("processMockCallback: CannotAcquireLockException propagates and does not modify entities")
    void processMockCallback_WhenLockAcquisitionFails_ShouldPropagateException() {
        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L))
                .thenThrow(new CannotAcquireLockException("Database lock acquisition timeout on order row"));

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-LOCK-FAIL");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        assertThatThrownBy(() -> paymentService.processMockCallback(request))
                .isInstanceOf(CannotAcquireLockException.class);

        assertThat(mockPayment.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(mockOrder.getOrderStatus()).isEqualTo("PENDING");
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(paymentCallbackRepository, never()).save(any());
    }

    // =========================================================================
    // Phase 3 - Exact Cart Finalization & Failure Isolation Tests
    // =========================================================================

    @Test
    @DisplayName("Phase 3 - Test 3: Late success on Order A finalizes linked Cart A while active Cart B remains ACTIVE")
    void processMockCallback_WhenLateSuccessOnOrderA_ShouldCheckoutCartAAndLeaveCartBUntouched() {
        Cart cartA = Cart.builder()
                .cartId(10L)
                .customer(mockCustomer)
                .status("PENDING_PAYMENT")
                .build();
        Cart cartB = Cart.builder()
                .cartId(20L)
                .customer(mockCustomer)
                .status("ACTIVE")
                .build();

        mockOrder.setCart(cartA);

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-LATE-CART-A")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-LATE-CART-A");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(cartA.getStatus()).isEqualTo("CHECKED_OUT");
        verify(cartRepository).save(cartA);

        // Cart B is completely untouched
        assertThat(cartB.getStatus()).isEqualTo("ACTIVE");
        verify(cartRepository, never()).save(cartB);
        verify(cartRepository, never()).findByCustomerCustomerIdAndStatus(anyLong(), anyString());
    }

    @Test
    @DisplayName("Phase 3 - Test 4: Multiple pending orders remain isolated; Payment A success only affects Cart A")
    void processMockCallback_WhenMultiplePendingOrders_PaymentASuccessOnlyAffectsCartA() {
        Cart cartA = Cart.builder().cartId(10L).customer(mockCustomer).status("PENDING_PAYMENT").build();
        Cart cartB = Cart.builder().cartId(20L).customer(mockCustomer).status("PENDING_PAYMENT").build();
        Cart cartC = Cart.builder().cartId(30L).customer(mockCustomer).status("ACTIVE").build();

        mockOrder.setCart(cartA);

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-MULTI-1")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-MULTI-1");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(cartA.getStatus()).isEqualTo("CHECKED_OUT");
        verify(cartRepository).save(cartA);

        // Other carts remain completely untouched
        assertThat(cartB.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(cartC.getStatus()).isEqualTo("ACTIVE");
        verify(cartRepository, never()).save(cartB);
        verify(cartRepository, never()).save(cartC);
        verify(cartRepository, never()).findByCustomerCustomerIdAndStatus(anyLong(), anyString());
    }

    @Test
    @DisplayName("Phase 3 - Test 6: Legacy CART order with cart = null does not corrupt active Cart B")
    void processMockCallback_WhenLegacyCartOrderWithNullCart_ShouldNotTouchAnyCart() {
        mockOrder.setCart(null);
        mockOrder.setCheckoutType("CART");

        Cart activeCartB = Cart.builder().cartId(99L).customer(mockCustomer).status("ACTIVE").build();

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-LEGACY-1")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-LEGACY-1");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(mockOrder.getOrderStatus()).isEqualTo("PAID");
        assertThat(mockOrder.getPaymentStatus()).isEqualTo("SUCCESS");

        // Active cart B is never touched and no cart is saved
        assertThat(activeCartB.getStatus()).isEqualTo("ACTIVE");
        verify(cartRepository, never()).save(any());
        verify(cartRepository, never()).findByCustomerCustomerIdAndStatus(anyLong(), anyString());
        verify(eventPublisher).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    @DisplayName("Phase 3 - Test 8: FAILED callback does not modify Cart B and preserves Cart A in PENDING_PAYMENT")
    void processMockCallback_WhenFailedCallback_DoesNotModifyCartBAndPreservesCartA() {
        Cart cartA = Cart.builder().cartId(10L).customer(mockCustomer).status("PENDING_PAYMENT").build();
        Cart cartB = Cart.builder().cartId(20L).customer(mockCustomer).status("ACTIVE").build();

        mockOrder.setCart(cartA);

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-FAIL-CART")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-FAIL-CART");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("FAILED");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(mockOrder.getPaymentStatus()).isEqualTo("FAILED");

        // Cart A is not restored to ACTIVE, Cart B is untouched
        assertThat(cartA.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(cartB.getStatus()).isEqualTo("ACTIVE");
        verify(cartRepository, never()).save(any());
        verify(cartRepository, never()).findByCustomerCustomerIdAndStatus(anyLong(), anyString());
    }

    // ==========================================
    // PHASE 10: CANCEL PAYMENT & CART RESTORE TESTS
    // ==========================================

    @Test
    @DisplayName("Phase 10: cancelPayment marks order CANCELLED, releases reservation, and auto-restores Cart A")
    void cancelPayment_success_marksOrderCancelled_releasesReservation_autoRestoresCart() {
        Cart cartA = Cart.builder().cartId(10L).customer(mockCustomer).status("PENDING_PAYMENT").build();
        mockOrder.setCart(cartA);
        mockOrder.setCheckoutType("CART");

        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.findFirstByOrderOrderIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.of(mockPayment));
        when(cartService.restoreFailedCartOrder(1L, 100L)).thenReturn(RestoreToCartResponse.builder().orderId(100L).restoredItemCount(2).build());

        PaymentCancelResponse response = paymentService.cancelPayment(1L, 100L);

        assertThat(response.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(response.getPaymentStatus()).isEqualTo("CANCELLED");
        assertThat(response.isItemsRestored()).isTrue();
        assertThat(mockOrder.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(mockOrder.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(mockPayment.getPaymentStatus()).isEqualTo("FAILED");

        verify(orderRepository).save(mockOrder);
        verify(paymentRepository).save(mockPayment);
        verify(reservationService).releaseReservationsForOrder(100L);
        verify(cartService).restoreFailedCartOrder(1L, 100L);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Phase 10: cancelPayment for BUY_NOW releases reservation and cancels order without cart restore")
    void cancelPayment_buyNowOrder_doesNotRestoreToCart() {
        mockOrder.setCart(null);
        mockOrder.setCheckoutType("BUY_NOW");

        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.findFirstByOrderOrderIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.of(mockPayment));

        PaymentCancelResponse response = paymentService.cancelPayment(1L, 100L);

        assertThat(response.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(response.isItemsRestored()).isFalse();
        assertThat(mockOrder.getOrderStatus()).isEqualTo("CANCELLED");

        verify(reservationService).releaseReservationsForOrder(100L);
        verify(cartService, never()).restoreFailedCartOrder(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Phase 10: cancelPayment throws ConflictException when order is already PAID")
    void cancelPayment_alreadyPaid_throwsConflictException() {
        mockOrder.setOrderStatus("PAID");
        mockOrder.setPaymentStatus("SUCCESS");
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> paymentService.cancelPayment(1L, 100L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already paid");

        verify(reservationService, never()).releaseReservationsForOrder(anyLong());
        verify(cartService, never()).restoreFailedCartOrder(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Phase 10: cancelPayment throws AccessDeniedException when customer does not own order")
    void cancelPayment_differentCustomer_throwsAccessDenied() {
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> paymentService.cancelPayment(999L, 100L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("belongs to a different customer");
    }

    @Test
    @DisplayName("Phase 10: cancelPayment when already CANCELLED is idempotent")
    void cancelPayment_alreadyCancelled_isIdempotent() {
        Cart cartA = Cart.builder().cartId(10L).customer(mockCustomer).status("RESTORED").build();
        mockOrder.setCart(cartA);
        mockOrder.setOrderStatus("CANCELLED");
        mockOrder.setPaymentStatus("FAILED");

        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        PaymentCancelResponse response = paymentService.cancelPayment(1L, 100L);

        assertThat(response.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(response.getMessage()).contains("already cancelled");
        verify(orderRepository, never()).save(any());
        verify(cartService, never()).restoreFailedCartOrder(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Phase 10: processMockCallback with CANCELLED status marks order CANCELLED and auto-restores cart")
    void processMockCallback_withCancelledStatus_marksOrderCancelled_andRestoresCart() {
        Cart cartA = Cart.builder().cartId(10L).customer(mockCustomer).status("PENDING_PAYMENT").build();
        mockOrder.setCart(cartA);
        mockOrder.setCheckoutType("CART");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-CANCEL-001")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-CANCEL-001");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("CANCELLED");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("CANCELLED");
        assertThat(mockOrder.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(mockOrder.getPaymentStatus()).isEqualTo("FAILED");

        verify(reservationService).releaseReservationsForOrder(100L);
        verify(cartService).restoreFailedCartOrder(1L, 100L);
    }

    @Test
    @DisplayName("Phase 10: processMockCallback with FAILED and cancel failureReason marks order CANCELLED and auto-restores")
    void processMockCallback_withFailureReasonCancel_marksOrderCancelled_andRestoresCart() {
        Cart cartA = Cart.builder().cartId(10L).customer(mockCustomer).status("PENDING_PAYMENT").build();
        mockOrder.setCart(cartA);
        mockOrder.setCheckoutType("CART");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-CANCEL-002")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-CANCEL-002");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("FAILED");
        request.setFailureReason("User cancelled payment on customer portal");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("CANCELLED");
        assertThat(mockOrder.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(mockOrder.getPaymentStatus()).isEqualTo("FAILED");

        verify(reservationService).releaseReservationsForOrder(100L);
        verify(cartService).restoreFailedCartOrder(1L, 100L);
    }

    @Test
    @DisplayName("Final UX Alignment: processMockCallback with FAILED status releases reservation and auto-restores cart")
    void processMockCallback_withFailedStatus_marksOrderCancelled_andAutoRestoresCart() {
        Cart cartA = Cart.builder().cartId(10L).customer(mockCustomer).status("PENDING_PAYMENT").build();
        mockOrder.setCart(cartA);
        mockOrder.setCheckoutType("CART");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-FAIL-001")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-FAIL-001");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("FAILED");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(mockOrder.getPaymentStatus()).isEqualTo("FAILED");

        verify(reservationService).releaseReservationsForOrder(100L);
        verify(cartService).restoreFailedCartOrder(1L, 100L);
    }

    @Test
    @DisplayName("Phase 10: processMockCallback when order already PAID ignores CANCEL and preserves SUCCESS")
    void processMockCallback_cancelled_whenOrderAlreadyPaid_preservesSuccess() {
        mockOrder.setOrderStatus("PAID");
        mockOrder.setPaymentStatus("SUCCESS");
        mockPayment.setPaymentStatus("SUCCESS");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-CANCEL-PAID")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-CANCEL-PAID");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("CANCELLED");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertThat(response.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(mockOrder.getOrderStatus()).isEqualTo("PAID");
        assertThat(mockOrder.getPaymentStatus()).isEqualTo("SUCCESS");

        verify(reservationService, never()).releaseReservationsForOrder(anyLong());
        verify(cartService, never()).restoreFailedCartOrder(anyLong(), anyLong());
    }
}
