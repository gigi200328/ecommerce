package com.ojt.ecommerce.storefront.payment.controller;

import com.ojt.ecommerce.storefront.payment.dto.MockCallbackRequest;
import com.ojt.ecommerce.storefront.payment.dto.MockCallbackResponse;
import com.ojt.ecommerce.storefront.payment.dto.PaymentDetailResponse;
import com.ojt.ecommerce.storefront.payment.dto.PaymentInitiateRequest;
import com.ojt.ecommerce.storefront.payment.dto.PaymentInitiateResponse;
import com.ojt.ecommerce.storefront.payment.service.PaymentService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private CustomerPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new CustomerPrincipal(1L, "customer@example.com");
    }

    @Test
    void initiatePayment_returnsOkAndDelegatesCustomerOrderAndRequest() {
        PaymentInitiateRequest request = new PaymentInitiateRequest();
        request.setPaymentMethod("MINI_BANKING");
        PaymentInitiateResponse serviceResponse = PaymentInitiateResponse.builder()
                .paymentId(10L)
                .orderId(100L)
                .paymentStatus("PENDING")
                .amount(new BigDecimal("25000"))
                .redirectUrl("https://gateway.test/pay")
                .build();
        when(paymentService.initiatePayment(1L, 100L, request)).thenReturn(serviceResponse);

        ResponseEntity<PaymentInitiateResponse> response =
                paymentController.initiatePayment(principal, 100L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(paymentService).initiatePayment(1L, 100L, request);
    }

    @Test
    void getLatestPayment_whenPaymentExists_returnsOk() {
        PaymentDetailResponse serviceResponse = PaymentDetailResponse.builder()
                .paymentId(10L)
                .orderId(100L)
                .paymentStatus("SUCCESS")
                .paymentMethod("MINI_BANKING")
                .transactionRef("TXN-001")
                .amount(new BigDecimal("25000"))
                .build();
        when(paymentService.getLatestPayment(1L, 100L)).thenReturn(serviceResponse);

        ResponseEntity<PaymentDetailResponse> response =
                paymentController.getLatestPayment(principal, 100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(paymentService).getLatestPayment(1L, 100L);
    }

    @Test
    void getLatestPayment_whenNoPaymentExists_returnsNoContent() {
        when(paymentService.getLatestPayment(1L, 100L)).thenReturn(null);

        ResponseEntity<PaymentDetailResponse> response =
                paymentController.getLatestPayment(principal, 100L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(paymentService).getLatestPayment(1L, 100L);
    }

    @Test
    void processMockCallback_returnsOkAndDelegatesRequest() {
        MockCallbackRequest request = new MockCallbackRequest();
        request.setExternalEventId("EVT-001");
        request.setTransactionRef("TXN-001");
        request.setStatus("SUCCESS");
        MockCallbackResponse serviceResponse = MockCallbackResponse.builder()
                .orderId(100L)
                .paymentStatus("SUCCESS")
                .build();
        when(paymentService.processMockCallback(request)).thenReturn(serviceResponse);

        ResponseEntity<MockCallbackResponse> response = paymentController.processMockCallback(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(serviceResponse, response.getBody());
        verify(paymentService).processMockCallback(request);
    }

    @Test
    void initiatePayment_propagatesServiceException() {
        PaymentInitiateRequest request = new PaymentInitiateRequest();
        RuntimeException expected = new RuntimeException("Order not found");
        when(paymentService.initiatePayment(1L, 404L, request)).thenThrow(expected);

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> paymentController.initiatePayment(principal, 404L, request));

        assertSame(expected, actual);
    }
}
