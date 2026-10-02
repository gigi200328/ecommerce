package com.ojt.ecommerce.storefront.payment.service;



import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.storefront.exception.AccessDeniedException;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.cart.repository.CartRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.payment.dto.*;
import com.ojt.ecommerce.storefront.payment.port.InventoryFinalizationPort;
import com.ojt.ecommerce.storefront.payment.repository.PaymentCallbackRepository;
import com.ojt.ecommerce.storefront.payment.repository.G5PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final G5PaymentRepository paymentRepository;
    private final PaymentCallbackRepository paymentCallbackRepository;
    private final G5OrderRepository orderRepository;
    private final G5OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CartRepository cartRepository;
    
    private final InventoryFinalizationPort inventoryFinalizationPort;

    @Override
    @Transactional
    public PaymentInitiateResponse initiatePayment(Long customerId, Long orderId, PaymentInitiateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
                
        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("Order belongs to a different customer");
        }
        
        if (!"PENDING".equals(order.getPaymentStatus()) && !"FAILED".equals(order.getPaymentStatus())) {
            throw new InvalidRequestException("Order is not payable");
        }
        
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setTransactionRef(UUID.randomUUID().toString());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentStatus("PENDING");
        payment.setAmount(order.getTotalAmount());
        payment.setCreatedAt(LocalDateTime.now());
        
        payment = paymentRepository.save(payment);
        
        return PaymentInitiateResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(orderId)
                .paymentStatus(payment.getPaymentStatus())
                .amount(payment.getAmount())
                .redirectUrl("/mock-g3/" + payment.getTransactionRef())
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
        if (paymentCallbackRepository.existsByExternalEventId(request.getExternalEventId())) {
            // Already processed this exact callback event idempotently
            Payment p = paymentRepository.findByTransactionRef(request.getTransactionRef())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
            return MockCallbackResponse.builder()
                    .orderId(p.getOrder().getOrderId())
                    .paymentStatus(p.getPaymentStatus())
                    .build();
        }
        
        Payment payment = paymentRepository.findByTransactionRef(request.getTransactionRef())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
                
        PaymentCallback callback = new PaymentCallback();
        callback.setPayment(payment);
        callback.setExternalEventId(request.getExternalEventId());
        callback.setTransactionRef(request.getTransactionRef());
        callback.setEventType(request.getStatus());
        callback.setSignatureValid(true);
        callback.setPayloadJson("{\"status\":\"" + request.getStatus() + "\"}");
        callback.setReceivedAt(LocalDateTime.now());
        
        if ("SUCCESS".equals(payment.getPaymentStatus())) {
            // Idempotent: payment already successful from a different callback event
            callback.setProcessingStatus("PROCESSED");
            callback.setProcessedAt(LocalDateTime.now());
            paymentCallbackRepository.save(callback);
            
            return MockCallbackResponse.builder()
                    .orderId(payment.getOrder().getOrderId())
                    .paymentStatus(payment.getPaymentStatus())
                    .build();
        }
        
        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            try {
                inventoryFinalizationPort.finalizeInventoryForOrder(payment.getOrder());
            } catch (InvalidRequestException ex) {
                // Stock validation failed
                callback.setProcessingStatus("REJECTED");
                callback.setProcessedAt(LocalDateTime.now());
                paymentCallbackRepository.save(callback);
                
                payment.setPaymentStatus("FAILED");
            Order order = payment.getOrder();
            order.setPaymentStatus("FAILED");
            orderRepository.save(order);                paymentRepository.save(payment);
                
                throw ex; // rollback the transaction and fail
            }
            
            payment.setPaymentStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
            
            Order order = payment.getOrder();
            order.setPaymentStatus("SUCCESS");
            order.setOrderStatus("PAID");
            orderRepository.save(order);
            
            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setOldStatus("PENDING");
            history.setNewStatus("PAID");
            history.setChangedAt(LocalDateTime.now());
            history.setRemark("Payment successful via mock callback");
            orderStatusHistoryRepository.save(history);
            
            Cart cart = cartRepository.findByCustomerCustomerIdAndStatus(order.getCustomer().getCustomerId(), "ACTIVE")
                    .orElse(null);
            if (cart != null) {
                cart.setStatus("CHECKED_OUT");
                cartRepository.save(cart);
            }
            
            callback.setProcessingStatus("PROCESSED");
            callback.setProcessedAt(LocalDateTime.now());
            paymentCallbackRepository.save(callback);
            
            return MockCallbackResponse.builder()
                    .orderId(order.getOrderId())
                    .paymentStatus("SUCCESS")
                    .build();
        } else {
            payment.setPaymentStatus("FAILED");
            Order order = payment.getOrder();
            order.setPaymentStatus("FAILED");
            orderRepository.save(order);            paymentRepository.save(payment);
            
            callback.setProcessingStatus("PROCESSED");
            callback.setProcessedAt(LocalDateTime.now());
            paymentCallbackRepository.save(callback);
            
            return MockCallbackResponse.builder()
                    .orderId(payment.getOrder().getOrderId())
                    .paymentStatus("FAILED")
                    .build();
        }
    }
}
