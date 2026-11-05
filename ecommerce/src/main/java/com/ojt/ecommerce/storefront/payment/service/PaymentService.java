package com.ojt.ecommerce.storefront.payment.service;
import com.ojt.ecommerce.storefront.payment.dto.*;
public interface PaymentService {
    PaymentInitiateResponse initiatePayment(Long customerId, Long orderId, PaymentInitiateRequest request);
    PaymentDetailResponse getLatestPayment(Long customerId, Long orderId);
    MockCallbackResponse processMockCallback(MockCallbackRequest request);
}