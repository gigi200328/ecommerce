package com.ojt.ecommerce.storefront.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.backofficeinventory.service.InventoryService;
import com.ojt.ecommerce.storefront.cart.repository.CartRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.payment.dto.*;
import com.ojt.ecommerce.storefront.payment.event.OrderPaidEvent;
import com.ojt.ecommerce.storefront.payment.repository.G5PaymentRepository;
import com.ojt.ecommerce.storefront.payment.repository.PaymentCallbackRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

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

        // Inject configuration values into paymentService
        ReflectionTestUtils.setField(paymentService, "pgBaseUrl", "http://localhost:8004/api/v1");
        ReflectionTestUtils.setField(paymentService, "pgClientId", "test-client");
        ReflectionTestUtils.setField(paymentService, "pgBranchId", "test-branch");
        ReflectionTestUtils.setField(paymentService, "pgClientSecret", "test-secret");
        paymentService.setRestTemplate(restTemplate);
    }

    // -------------------------------------------------------------
    // Scenario 1: Callback obtains Order-level lock
    // -------------------------------------------------------------
    @Test
    @DisplayName("Scenario 1: Callback processing obtains Order-level lock via findByIdWithLock")
    void callback_obtainsOrderLevelLock() {
        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-LOCK-1")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-LOCK-1");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        paymentService.processMockCallback(request);

        // Verify Order-level exclusive lock was requested before status updates
        verify(orderRepository, times(1)).findByIdWithLock(100L);
    }

    // -------------------------------------------------------------
    // Scenario 2: Payment initiation obtains Order-level lock
    // -------------------------------------------------------------
    @Test
    @DisplayName("Scenario 2: Payment initiation obtains Order-level lock via findByIdWithLock")
    void paymentInitiation_obtainsOrderLevelLock() {
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        
        MmspgInitiateResponse mockGatewayResponse = new MmspgInitiateResponse();
        mockGatewayResponse.setTransactionReference("TXN-NEW-001");
        mockGatewayResponse.setPaymentUrl("http://localhost:8004/checkout?token=xyz");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(MmspgInitiateResponse.class)))
                .thenReturn(new ResponseEntity<>(mockGatewayResponse, HttpStatus.OK));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentInitiateRequest request = new PaymentInitiateRequest();
        request.setPaymentMethod("MINI_BANKING");

        PaymentInitiateResponse response = paymentService.initiatePayment(1L, 100L, request);

        assertNotNull(response);
        verify(orderRepository, times(1)).findByIdWithLock(100L);
    }

    // -------------------------------------------------------------
    // Scenario 3: Delayed FAILED callback preserves PAID / SUCCESS
    // -------------------------------------------------------------
    @Test
    @DisplayName("Scenario 3: Delayed FAILED callback after successful payment retry preserves PAID/SUCCESS order")
    void delayedFailedCallback_afterSuccessfulRetry_preservesPaidOrder() {
        mockOrder.setOrderStatus("PAID");
        mockOrder.setPaymentStatus("SUCCESS");
        mockPayment.setPaymentStatus("PENDING");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-FAILED-1")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-FAILED-1");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("FAILED");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals("FAILED", response.getPaymentStatus());

        assertEquals("FAILED", mockPayment.getPaymentStatus());
        verify(paymentRepository).save(mockPayment);

        assertEquals("PAID", mockOrder.getOrderStatus());
        assertEquals("SUCCESS", mockOrder.getPaymentStatus());
        verify(orderRepository, never()).save(mockOrder);

        verify(inventoryService, never()).deductStockForOrder(anyLong());
        verify(cartRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(paymentCallbackRepository).save(any(PaymentCallback.class));
    }

    // -------------------------------------------------------------
    // Scenario 4: Late SUCCESS callback skips duplicate inventory deduction
    // -------------------------------------------------------------
    @Test
    @DisplayName("Scenario 4: Late SUCCESS callback on already-paid order avoids duplicate inventory deduction and cart deactivation")
    void lateSuccessCallback_alreadyPaidOrder_skipsDuplicateSideEffects() {
        mockOrder.setOrderStatus("PAID");
        mockOrder.setPaymentStatus("SUCCESS");
        mockOrder.setCheckoutType("CART");
        mockPayment.setPaymentStatus("PENDING");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-LATE-SUCCESS")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-LATE-SUCCESS");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals("SUCCESS", response.getPaymentStatus());

        assertEquals("SUCCESS", mockPayment.getPaymentStatus());
        verify(paymentRepository).save(mockPayment);

        verify(orderRepository, never()).save(mockOrder);
        verify(orderStatusHistoryRepository, never()).save(any());
        verify(cartRepository, never()).save(any());
        verify(inventoryService, never()).deductStockForOrder(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
        verify(paymentCallbackRepository).save(any(PaymentCallback.class));
    }

    // -------------------------------------------------------------
    // Scenario 5: Already-paid Order cannot initiate another payment
    // -------------------------------------------------------------
    @Test
    @DisplayName("Scenario 5: Already-paid Order cannot initiate another payment")
    void alreadyPaidOrder_cannotInitiatePayment() {
        mockOrder.setOrderStatus("PAID");
        mockOrder.setPaymentStatus("SUCCESS");

        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        PaymentInitiateRequest request = new PaymentInitiateRequest();
        request.setPaymentMethod("MINI_BANKING");

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> paymentService.initiatePayment(1L, 100L, request)
        );

        assertEquals("Order is already paid", exception.getMessage());
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        verify(paymentRepository, never()).save(any());
    }

    // -------------------------------------------------------------
    // Scenario 6: Valid unpaid Order can still initiate payment
    // -------------------------------------------------------------
    @Test
    @DisplayName("Scenario 6: Valid unpaid Order can still initiate payment")
    void validUnpaidOrder_canInitiatePayment() {
        mockOrder.setOrderStatus("PENDING");
        mockOrder.setPaymentStatus("PENDING");

        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        MmspgInitiateResponse mockGatewayResponse = new MmspgInitiateResponse();
        mockGatewayResponse.setTransactionReference("TXN-NEW-002");
        mockGatewayResponse.setPaymentUrl("http://localhost:8004/checkout?token=abc");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(MmspgInitiateResponse.class)))
                .thenReturn(new ResponseEntity<>(mockGatewayResponse, HttpStatus.OK));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentInitiateRequest request = new PaymentInitiateRequest();
        request.setPaymentMethod("MINI_BANKING");

        PaymentInitiateResponse response = paymentService.initiatePayment(1L, 100L, request);

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals("PENDING", response.getPaymentStatus());
        assertEquals("http://localhost:8004/checkout?token=abc", response.getRedirectUrl());
        verify(paymentRepository).save(any(Payment.class));
    }

    // -------------------------------------------------------------
    // Scenario 7: BUY_NOW preserves active cart
    // -------------------------------------------------------------
    @Test
    @DisplayName("Scenario 7: BUY_NOW order payment success preserves active cart")
    void buyNowOrder_successCallback_preservesActiveCart() {
        mockOrder.setOrderStatus("PENDING");
        mockOrder.setPaymentStatus("PENDING");
        mockOrder.setCheckoutType("BUY_NOW");
        mockPayment.setPaymentStatus("PENDING");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-BUYNOW-1")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-BUYNOW-1");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertEquals("SUCCESS", response.getPaymentStatus());
        assertEquals("PAID", mockOrder.getOrderStatus());
        assertEquals("SUCCESS", mockOrder.getPaymentStatus());

        verify(cartRepository, never()).findByCustomerCustomerIdAndStatus(anyLong(), anyString());
        verify(cartRepository, never()).save(any());
        // Synchronous inventory deduction removed from callback transaction; OrderPaidEvent published
        verify(inventoryService, never()).deductStockForOrder(anyLong());
        verify(eventPublisher, times(1)).publishEvent(any(OrderPaidEvent.class));
    }

    // -------------------------------------------------------------
    // Scenario 8: Normal CART success behavior remains unchanged
    // -------------------------------------------------------------
    @Test
    @DisplayName("Scenario 8: Normal CART order payment success cleans up active cart")
    void cartOrder_successCallback_cleansUpActiveCart() {
        mockOrder.setOrderStatus("PENDING");
        mockOrder.setPaymentStatus("PENDING");
        mockOrder.setCheckoutType("CART");
        mockPayment.setPaymentStatus("PENDING");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-CART-1")).thenReturn(false);
        when(cartRepository.findByCustomerCustomerIdAndStatus(1L, "ACTIVE")).thenReturn(Optional.of(mockCart));

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-CART-1");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertEquals("SUCCESS", response.getPaymentStatus());
        assertEquals("CHECKED_OUT", mockCart.getStatus());
        verify(cartRepository).save(mockCart);
        // Synchronous inventory deduction removed from callback transaction; OrderPaidEvent published
        verify(inventoryService, never()).deductStockForOrder(anyLong());
        verify(eventPublisher, times(1)).publishEvent(any(OrderPaidEvent.class));
    }

    // -------------------------------------------------------------
    // Scenario 9: Callback with mismatched payment/order association is rejected safely
    // -------------------------------------------------------------
    @Test
    @DisplayName("Scenario 9: Callback with mismatched payment/order association is rejected safely")
    void callback_withMismatchedPaymentOrderAssociation_isRejectedSafely() {
        Order mismatchedOrder = Order.builder()
                .orderId(999L)
                .orderNo("ORD-DIFF-001")
                .customer(mockCustomer)
                .build();

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        // Repository returns an order with id 999 instead of mockPayment's associated orderId 100
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mismatchedOrder));

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-MISMATCH");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> paymentService.processMockCallback(request)
        );

        assertEquals("Payment and Order association mismatch", exception.getMessage());
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(inventoryService, never()).deductStockForOrder(anyLong());
    }

    // -------------------------------------------------------------
    // Scenario 10: Lock acquisition failure does not produce a misleading successful callback result
    // -------------------------------------------------------------
    @Test
    @DisplayName("Scenario 10: Lock acquisition failure does not produce a misleading successful callback result")
    void lockAcquisitionFailure_doesNotProduceMisleadingSuccess() {
        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L))
                .thenThrow(new CannotAcquireLockException("Database lock acquisition timeout on order row"));

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-LOCK-FAIL");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("SUCCESS");

        assertThrows(
                CannotAcquireLockException.class,
                () -> paymentService.processMockCallback(request)
        );

        // Verify that neither payment nor order was marked SUCCESS or saved
        assertEquals("PENDING", mockPayment.getPaymentStatus());
        assertEquals("PENDING", mockOrder.getOrderStatus());
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(inventoryService, never()).deductStockForOrder(anyLong());
        verify(paymentCallbackRepository, never()).save(any());
    }

    // -------------------------------------------------------------
    // Existing Regressions
    // -------------------------------------------------------------
    @Test
    @DisplayName("Normal Flow: Normal FAILED callback on unpaid order marks payment and order as FAILED")
    void normalFailedCallback_unpaidOrder_setsOrderFailed() {
        mockOrder.setOrderStatus("PENDING");
        mockOrder.setPaymentStatus("PENDING");
        mockPayment.setPaymentStatus("PENDING");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-FAILED-2")).thenReturn(false);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-FAILED-2");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("FAILED");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals("FAILED", response.getPaymentStatus());

        assertEquals("FAILED", mockPayment.getPaymentStatus());
        verify(paymentRepository).save(mockPayment);

        assertEquals("FAILED", mockOrder.getPaymentStatus());
        verify(orderRepository).save(mockOrder);

        verify(inventoryService, never()).deductStockForOrder(anyLong());
        verify(paymentCallbackRepository).save(any(PaymentCallback.class));
    }

    @Test
    @DisplayName("Idempotency: Duplicate FAILED callback returns existing status idempotently")
    void duplicateFailedCallback_handledIdempotently() {
        mockPayment.setPaymentStatus("FAILED");

        when(paymentRepository.findByTransactionRef("TXN-TEST-001")).thenReturn(Optional.of(mockPayment));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(paymentCallbackRepository.existsByExternalEventId("EVT-DUP-1")).thenReturn(true);

        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-DUP-1");
        request.setTransactionRef("TXN-TEST-001");
        request.setStatus("FAILED");

        MockCallbackResponse response = paymentService.processMockCallback(request);

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals("FAILED", response.getPaymentStatus());

        verify(orderRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Payment Guard: FAILED callback on an already SUCCESS payment does not downgrade the payment")
    void failedCallback_onPaymentAlreadySuccess_doesNotDowngradePayment() {
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

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals("SUCCESS", response.getPaymentStatus());
        assertEquals("SUCCESS", mockPayment.getPaymentStatus());
        verify(orderRepository, never()).save(any());
    }
}
