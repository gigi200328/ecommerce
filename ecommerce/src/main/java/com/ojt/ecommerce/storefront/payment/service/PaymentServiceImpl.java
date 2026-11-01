package com.ojt.ecommerce.storefront.payment.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.backofficeinventory.service.InventoryService;
import com.ojt.ecommerce.storefront.exception.AccessDeniedException;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.cart.repository.CartRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.payment.dto.*;
import com.ojt.ecommerce.storefront.payment.repository.PaymentCallbackRepository;
import com.ojt.ecommerce.storefront.payment.repository.G5PaymentRepository;
import lombok.RequiredArgsConstructor;

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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final G5PaymentRepository paymentRepository;
    private final PaymentCallbackRepository paymentCallbackRepository;
    private final G5OrderRepository orderRepository;
    private final G5OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CartRepository cartRepository;
    private final InventoryService inventoryService;
    
    private final ApplicationEventPublisher eventPublisher;
    
    @Value("${payment.gateway.base-url}")
    private String pgBaseUrl;

    @Value("${payment.gateway.client-id}")
    private String pgClientId;

    @Value("${payment.gateway.branch-id}")
    private String pgBranchId;

    @Value("${payment.gateway.client-secret}")
    private String pgClientSecret;

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

        // 🔴 2. Save PG's transaction reference & token in E-commerce DB
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setTransactionRef(gatewayData.getTransactionReference()); // PG transaction ref (TXN-...)
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentStatus("PENDING");
        payment.setAmount(order.getTotalAmount());
        payment.setCreatedAt(LocalDateTime.now());

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
                .map(payment -> PaymentDetailResponse.builder()
                        .paymentId(payment.getPaymentId())
                        .orderId(orderId)
                        .paymentStatus(payment.getPaymentStatus())
                        .paymentMethod(payment.getPaymentMethod())
                        .transactionRef(payment.getTransactionRef())
                        .amount(payment.getAmount())
                        .paidAt(payment.getPaidAt() != null ? payment.getPaidAt().toString() : null)
                        .createdAt(payment.getCreatedAt().toString())
                        .build())
                .orElse(null);
    }

    @Override
    @Transactional
    public MockCallbackResponse processMockCallback(MockCallbackRequest request) {
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
        callback.setSignatureValid(true);
        callback.setPayloadJson("{\"status\":\"" + request.getStatus() + "\"}");
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

                // 🔴 Only clean up active cart if checkout was CART-based (preserve cart for BUY_NOW)
                boolean isBuyNow = "BUY_NOW".equalsIgnoreCase(order.getCheckoutType());
                if (!isBuyNow) {
                    Cart cart = cartRepository.findByCustomerCustomerIdAndStatus(order.getCustomer().getCustomerId(), "ACTIVE")
                            .orElse(null);
                    if (cart != null) {
                        cart.setStatus("CHECKED_OUT");
                        cartRepository.save(cart);
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
            if (!"SUCCESS".equalsIgnoreCase(payment.getPaymentStatus())) {
                payment.setPaymentStatus("FAILED");
                paymentRepository.save(payment);
            }

            if (!isOrderAlreadyPaid) {
                order.setPaymentStatus("FAILED");
                orderRepository.save(order);
            }

            callback.setProcessingStatus("PROCESSED");
            callback.setProcessedAt(LocalDateTime.now());
            paymentCallbackRepository.save(callback);

            return MockCallbackResponse.builder()
                    .orderId(order.getOrderId())
                    .paymentStatus("FAILED")
                    .build();
        }
    }
}
