package com.ojt.ecommerce.storefront.payment.service;

import com.ojt.ecommerce.backofficeinventory.service.InventoryService;
import com.ojt.ecommerce.entity.Cart;
import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.entity.Payment;
import com.ojt.ecommerce.entity.PaymentCallback;
import com.ojt.ecommerce.storefront.cart.repository.CartRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.exception.AccessDeniedException;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.payment.dto.MmspgInitiateResponse;
import com.ojt.ecommerce.storefront.payment.dto.MockCallbackRequest;
import com.ojt.ecommerce.storefront.payment.dto.MockCallbackResponse;
import com.ojt.ecommerce.storefront.payment.dto.PaymentDetailResponse;
import com.ojt.ecommerce.storefront.payment.dto.PaymentInitiateRequest;
import com.ojt.ecommerce.storefront.payment.dto.PaymentInitiateResponse;
import com.ojt.ecommerce.storefront.payment.event.OrderPaidEvent;
import com.ojt.ecommerce.storefront.payment.repository.G5PaymentRepository;
import com.ojt.ecommerce.storefront.payment.repository.PaymentCallbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private InventoryService inventoryService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RestTemplate restTemplate;

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
    @DisplayName("processMockCallback: SUCCESS callback on CART order updates order, history, cleans cart, and publishes event")
    void processMockCallback_WhenSuccessStatusAndCartOrder_ShouldUpdateOrderSaveHistoryCheckoutCartAndPublishEvent() {
        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-CART-1")).thenReturn(false);
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(mockCart));

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
}
