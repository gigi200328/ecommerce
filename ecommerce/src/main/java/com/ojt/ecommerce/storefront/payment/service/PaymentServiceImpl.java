package com.ojt.ecommerce.storefront.payment.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.backofficeinventory.service.InventoryService;
import com.ojt.ecommerce.storefront.cart.service.CartService;
import com.ojt.ecommerce.storefront.exception.AccessDeniedException;
import com.ojt.ecommerce.storefront.exception.ConflictException;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.cart.repository.CartRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.payment.dto.*;
import com.ojt.ecommerce.storefront.payment.repository.PaymentCallbackRepository;
import com.ojt.ecommerce.storefront.payment.repository.G5PaymentRepository;
import com.ojt.ecommerce.storefront.reservation.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.ojt.ecommerce.storefront.payment.event.OrderPaidEvent;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojt.ecommerce.common.util.HmacSignatureUtil;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final G5PaymentRepository paymentRepository;
    private final PaymentCallbackRepository paymentCallbackRepository;
    private final G5OrderRepository orderRepository;
    private final G5OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final InventoryService inventoryService;
    private final InventoryReservationService reservationService;
    
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    
    @Value("${payment.gateway.base-url}")
    private String pgBaseUrl;

    @Value("${payment.gateway.client-id}")
    private String pgClientId;

    @Value("${payment.gateway.branch-id}")
    private String pgBranchId;

    @Value("${payment.gateway.client-secret}")
    private String pgClientSecret;

    @Value("${payment.gateway.webhook-secret:${payment.gateway.client-secret}}")
    private String pgWebhookSecret;

    private RestTemplate restTemplate = new RestTemplate();

    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    @Override
    @Transactional
    public PaymentInitiateResponse initiatePayment(Long customerId, Long orderId, PaymentInitiateRequest request) {
        // Acquire exclusive Order-level lock before evaluating payment eligibility
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Order belongs to a different customer");
        }

        // Revalidate Order status after locking: prevent payment initiation on already paid/successful orders
        if ("SUCCESS".equalsIgnoreCase(order.getPaymentStatus()) || "PAID".equalsIgnoreCase(order.getOrderStatus())) {
            throw new InvalidRequestException("Order is already paid");
        }

        if (!"PENDING".equals(order.getPaymentStatus()) && !"FAILED".equals(order.getPaymentStatus())) {
            throw new InvalidRequestException("Order is not payable");
        }

        String idempotencyKey = UUID.randomUUID().toString();

        // 🔴 1. Call Payment Gateway (MMSPG) API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Client-ID", pgClientId); // Configured in application.properties
        headers.set("X-Client-Secret", pgClientSecret); // Configured in application.properties
        headers.set("Idempotency-Key", idempotencyKey);

        Map<String, Object> pgRequest = Map.of(
                "orderId", String.valueOf(orderId),
                "amount", order.getTotalAmount(),
                "currency", "MMK",
                "branchId", pgBranchId // Or terminalId
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(pgRequest, headers);

        // Call: POST http://<pg-url>/api/v1/payments/initiate
        ResponseEntity<MmspgInitiateResponse> pgResponse = restTemplate.postForEntity(
                pgBaseUrl + "/payments/initiate",
                entity,
                MmspgInitiateResponse.class
        );

        MmspgInitiateResponse gatewayData = pgResponse.getBody();

        // 🔴 2. Save/reuse PG's transaction reference in E-commerce DB (Deduplication Safety)
        String txnRef = gatewayData.getTransactionReference();
        Payment payment = paymentRepository.findByTransactionRef(txnRef)
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setOrder(order);
                    p.setTransactionRef(txnRef);
                    p.setCreatedAt(LocalDateTime.now());
                    return p;
                });

        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentStatus("PENDING");
        payment.setAmount(order.getTotalAmount());

        payment = paymentRepository.save(payment);

        // 🔴 3. Return the real redirect URL from MMSPG!
        return PaymentInitiateResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(orderId)
                .paymentStatus(payment.getPaymentStatus())
                .amount(payment.getAmount())
                .redirectUrl(gatewayData.getPaymentUrl()) // e.g. https://customer-portal.../checkout?token=...
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailResponse getLatestPayment(Long customerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
                
        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Order belongs to a different customer");
        }
        
        return paymentRepository.findFirstByOrderOrderIdOrderByCreatedAtDesc(orderId)
                .map(payment -> {
                    String status = payment.getPaymentStatus();
                    if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
                        status = "CANCELLED";
                    }
                    return PaymentDetailResponse.builder()
                            .paymentId(payment.getPaymentId())
                            .orderId(orderId)
                            .paymentStatus(status)
                            .paymentMethod(payment.getPaymentMethod())
                            .transactionRef(payment.getTransactionRef())
                            .amount(payment.getAmount())
                            .paidAt(payment.getPaidAt() != null ? payment.getPaidAt().toString() : null)
                            .createdAt(payment.getCreatedAt().toString())
                            .build();
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public MockCallbackResponse processMockCallback(String rawPayload, String signature) {
        boolean isSignatureValid = HmacSignatureUtil.verifySignature(rawPayload, pgWebhookSecret, signature);

        MockCallbackRequest request;
        try {
            request = objectMapper.readValue(rawPayload, MockCallbackRequest.class);
        } catch (Exception e) {
            throw new InvalidRequestException("Malformed callback payload JSON: " + e.getMessage());
        }

        if (!isSignatureValid) {
            Payment payment = paymentRepository.findByTransactionRef(request.getTransactionRef()).orElse(null);
            if (payment != null) {
                PaymentCallback callback = new PaymentCallback();
                callback.setPayment(payment);
                callback.setExternalEventId(request.getExternalEventId());
                callback.setTransactionRef(request.getTransactionRef());
                callback.setEventType(request.getStatus());
                callback.setSignatureValid(false);
                callback.setPayloadJson(rawPayload);
                callback.setReceivedAt(LocalDateTime.now());
                callback.setProcessingStatus("REJECTED");
                callback.setProcessedAt(LocalDateTime.now());
                paymentCallbackRepository.save(callback);
            }
            throw new AccessDeniedException("Invalid payment callback signature! Request rejected.");
        }

        return executeCallback(request, rawPayload, true);
    }

    @Override
    @Transactional
    public MockCallbackResponse processMockCallback(MockCallbackRequest request) {
        return executeCallback(request, "{\"status\":\"" + request.getStatus() + "\"}", true);
    }

    private MockCallbackResponse executeCallback(MockCallbackRequest request, String payloadJson, boolean signatureValid) {
        // 1. Resolve and validate the callback's payment reference
        Payment payment = paymentRepository.findByTransactionRef(request.getTransactionRef())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getOrder() == null || payment.getOrder().getOrderId() == null) {
            throw new InvalidRequestException("Payment is not associated with an order");
        }

        Long orderId = payment.getOrder().getOrderId();

        // 2. Acquire an exclusive Order row lock within the active transaction
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // 3. Ensure the payment reference and locked Order association match
        if (!order.getOrderId().equals(payment.getOrder().getOrderId())) {
            throw new InvalidRequestException("Payment and Order association mismatch");
        }

        // 4. Recheck callback idempotency after acquiring the exclusive Order lock
        if (paymentCallbackRepository.existsByExternalEventId(request.getExternalEventId())) {
            return MockCallbackResponse.builder()
                    .orderId(order.getOrderId())
                    .paymentStatus(payment.getPaymentStatus())
                    .build();
        }

        PaymentCallback callback = new PaymentCallback();
        callback.setPayment(payment);
        callback.setExternalEventId(request.getExternalEventId());
        callback.setTransactionRef(request.getTransactionRef());
        callback.setEventType(request.getStatus());
        callback.setSignatureValid(signatureValid);
        callback.setPayloadJson(payloadJson);
        callback.setReceivedAt(LocalDateTime.now());

        // 5. If this specific payment was already recorded as SUCCESS
        if ("SUCCESS".equalsIgnoreCase(payment.getPaymentStatus())) {
            callback.setProcessingStatus("PROCESSED");
            callback.setProcessedAt(LocalDateTime.now());
            paymentCallbackRepository.save(callback);

            return MockCallbackResponse.builder()
                    .orderId(order.getOrderId())
                    .paymentStatus(payment.getPaymentStatus())
                    .build();
        }

        // 6. Re-evaluate the authoritative Order status after acquiring the lock
        boolean isOrderAlreadyPaid = "SUCCESS".equalsIgnoreCase(order.getPaymentStatus())
                || "PAID".equalsIgnoreCase(order.getOrderStatus());

        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            payment.setPaymentStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            if (!isOrderAlreadyPaid) {
                order.setPaymentStatus("SUCCESS");
                order.setOrderStatus("PAID");
                orderRepository.save(order);

                OrderStatusHistory history = new OrderStatusHistory();
                history.setOrder(order);
                history.setOldStatus("PENDING");
                history.setNewStatus("PAID");
                history.setChangedAt(LocalDateTime.now());
                history.setRemark("Payment successful via MMSPG webhook callback");
                orderStatusHistoryRepository.save(history);

                // Only finalize cart if checkout was CART-based (preserve cart for BUY_NOW)
                boolean isBuyNow = "BUY_NOW".equalsIgnoreCase(order.getCheckoutType());
                if (!isBuyNow) {
                    Cart cart = order.getCart();
                    if (cart != null) {
                        if ("PENDING_PAYMENT".equalsIgnoreCase(cart.getStatus())) {
                            cart.setStatus("CHECKED_OUT");
                            cart.setModifiedAt(LocalDateTime.now());
                            cartRepository.save(cart);
                        }
                    } else {
                        log.warn("Cart association is null for CART order #{} - skipping cart checkout finalization", order.getOrderId());
                    }
                }

                // Publish OrderPaidEvent -> Handled AFTER_COMMIT by OrderPaidEventListener in an isolated REQUIRES_NEW transaction
                eventPublisher.publishEvent(new OrderPaidEvent(
                        order.getOrderId(),
                        order.getOrderNo(),
                        payment.getTransactionRef(),
                        payment.getAmount(),
                        payment.getPaidAt()
                ));
            }

            callback.setProcessingStatus("PROCESSED");
            callback.setProcessedAt(LocalDateTime.now());
            paymentCallbackRepository.save(callback);

            return MockCallbackResponse.builder()
                    .orderId(order.getOrderId())
                    .paymentStatus("SUCCESS")
                    .build();
        } else {
            // Determine if this failure event is an explicit cancellation
            boolean isCancelled = "CANCELLED".equalsIgnoreCase(request.getStatus())
                    || (request.getFailureReason() != null && request.getFailureReason().toLowerCase().contains("cancel"))
                    || checkCancellationWithMmspg(request.getTransactionRef());

            if (!"SUCCESS".equalsIgnoreCase(payment.getPaymentStatus())) {
                payment.setPaymentStatus("FAILED");
                paymentRepository.save(payment);
            }

            if (!isOrderAlreadyPaid) {
                if (isCancelled) {
                    order.setOrderStatus("CANCELLED");
                    order.setPaymentStatus("FAILED");
                    orderRepository.save(order);

                    OrderStatusHistory history = new OrderStatusHistory();
                    history.setOrder(order);
                    history.setOldStatus("PENDING");
                    history.setNewStatus("CANCELLED");
                    history.setChangedAt(LocalDateTime.now());
                    history.setRemark("Payment cancelled by customer via payment gateway");
                    orderStatusHistoryRepository.save(history);
                } else {
                    order.setPaymentStatus("FAILED");
                    orderRepository.save(order);
                }
            }

            // Phase 7/10: Release active reservations for this order
            reservationService.releaseReservationsForOrder(order.getOrderId());

            // Phase 10 / Final UX Alignment: Auto-restore Cart A into Cart B for CART orders on failure or cancellation
            if (!isOrderAlreadyPaid && "CART".equalsIgnoreCase(order.getCheckoutType()) && order.getCart() != null) {
                try {
                    cartService.restoreFailedCartOrder(order.getCustomer().getCustomerId(), order.getOrderId());
                } catch (ConflictException ce) {
                    log.info("Cart already restored for failed/cancelled order #{}: {}", order.getOrderId(), ce.getMessage());
                } catch (Exception e) {
                    log.warn("Failed to auto-restore cart items for failed/cancelled order #{}: {}", order.getOrderId(), e.getMessage());
                }
            }

            callback.setProcessingStatus("PROCESSED");
            callback.setProcessedAt(LocalDateTime.now());
            paymentCallbackRepository.save(callback);

            return MockCallbackResponse.builder()
                    .orderId(order.getOrderId())
                    .paymentStatus(isCancelled ? "CANCELLED" : "FAILED")
                    .build();
        }
    }

    @Override
    @Transactional
    public PaymentCancelResponse cancelPayment(Long customerId, Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Order belongs to a different customer");
        }

        if ("PAID".equalsIgnoreCase(order.getOrderStatus()) || "SUCCESS".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new ConflictException("Cannot cancel an order that is already paid");
        }

        // Idempotency: if already cancelled, ensure reservation is released and return
        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            return PaymentCancelResponse.builder()
                    .orderId(orderId)
                    .orderStatus("CANCELLED")
                    .paymentStatus("CANCELLED")
                    .message("Order payment was already cancelled")
                    .itemsRestored(order.getCart() != null && "RESTORED".equalsIgnoreCase(order.getCart().getStatus()))
                    .build();
        }

        order.setOrderStatus("CANCELLED");
        order.setPaymentStatus("FAILED");
        orderRepository.save(order);

        paymentRepository.findFirstByOrderOrderIdOrderByCreatedAtDesc(orderId).ifPresent(p -> {
            if (!"SUCCESS".equalsIgnoreCase(p.getPaymentStatus())) {
                p.setPaymentStatus("FAILED");
                paymentRepository.save(p);
            }
        });

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldStatus("PENDING");
        history.setNewStatus("CANCELLED");
        history.setChangedAt(LocalDateTime.now());
        history.setRemark("Payment cancelled by customer");
        orderStatusHistoryRepository.save(history);

        // Release reservation
        reservationService.releaseReservationsForOrder(orderId);

        // Auto-restore Cart A into Cart B for CART orders only
        boolean itemsRestored = false;
        if ("CART".equalsIgnoreCase(order.getCheckoutType()) && order.getCart() != null) {
            try {
                cartService.restoreFailedCartOrder(customerId, orderId);
                itemsRestored = true;
            } catch (ConflictException ce) {
                log.info("Cart already restored for cancelled order #{}: {}", orderId, ce.getMessage());
                itemsRestored = true;
            } catch (Exception e) {
                log.warn("Failed to auto-restore cart items for cancelled order #{}: {}", orderId, e.getMessage());
            }
        }

        return PaymentCancelResponse.builder()
                .orderId(orderId)
                .orderStatus("CANCELLED")
                .paymentStatus("CANCELLED")
                .message("Payment cancelled successfully. Reserved stock has been released.")
                .itemsRestored(itemsRestored)
                .build();
    }

    private boolean checkCancellationWithMmspg(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) {
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Client-ID", pgClientId);
            headers.set("X-Client-Secret", pgClientSecret);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    pgBaseUrl + "/payments/status/" + transactionRef,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object reason = response.getBody().get("failureReason");
                if (reason != null && reason.toString().toLowerCase().contains("cancel")) {
                    return true;
                }
                Object status = response.getBody().get("status");
                if (status != null && status.toString().equalsIgnoreCase("CANCELLED")) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Could not query MMSPG transaction status for ref {}: {}", transactionRef, e.getMessage());
        }
        return false;
    }
}
