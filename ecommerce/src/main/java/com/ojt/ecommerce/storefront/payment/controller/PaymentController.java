package com.ojt.ecommerce.storefront.payment.controller;


import com.ojt.ecommerce.storefront.payment.dto.*;
import com.ojt.ecommerce.storefront.payment.service.PaymentService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storefront/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/payments/orders/{orderId}/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable Long orderId,
            @RequestBody PaymentInitiateRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(principal.getCustomerId(), orderId, request));
    }

    @GetMapping("/payments/orders/{orderId}/latest")
    public ResponseEntity<PaymentDetailResponse> getLatestPayment(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable Long orderId) {
        PaymentDetailResponse response = paymentService.getLatestPayment(principal.getCustomerId(), orderId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payment-callbacks/g3")
    public ResponseEntity<MockCallbackResponse> processMockCallback(
            @RequestBody MockCallbackRequest request) {
        return ResponseEntity.ok(paymentService.processMockCallback(request));
    }
}
