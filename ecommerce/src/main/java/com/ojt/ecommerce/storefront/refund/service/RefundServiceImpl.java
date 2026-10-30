package com.ojt.ecommerce.storefront.refund.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderItemRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.refund.dto.*;
import com.ojt.ecommerce.storefront.refund.repository.RefundRequestRepository;
import com.ojt.ecommerce.storefront.shipping.repository.G5ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ojt.ecommerce.backofficeinventory.service.AdminNotificationService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final G5OrderItemRepository orderItemRepository;
    private final G5ShipmentRepository shipmentRepository;
    private final G5OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final AdminNotificationService adminNotificationService;

    private static final int REFUND_WINDOW_DAYS = 7;

    private final List<String> VALID_REASONS = Arrays.asList(
        "DAMAGED_ITEM", "DEFECTIVE_PRODUCT", "MISSING_PARTS", 
        "ORDERED_BY_MISTAKE", "WRONG_ITEM_SENT", "OTHER"
    );

    @Override
    @Transactional
    public RefundResponseDto createRefundRequest(Long customerId, RefundRequestDto dto) {
        if (!VALID_REASONS.contains(dto.getRequestReason())) {
            throw new InvalidRequestException("Invalid refund reason");
        }
        if (dto.getRefundQuantity() == null || dto.getRefundQuantity() <= 0) {
            throw new InvalidRequestException("Refund quantity must be positive");
        }

        OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found"));
                
        Order order = orderItem.getOrder();
        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("OrderItem not found"); // Prevents IDOR
        }

        if (!"SUCCESS".equals(order.getPaymentStatus())) {
            throw new InvalidRequestException("Order is not eligible for refund (must be PAID)");
        }

        // Rule 1: Refund is only allowed AFTER order is DELIVERED
        LocalDateTime deliveredAt = getDeliveryTimestampIfDelivered(order);
        if (deliveredAt == null) {
            throw new InvalidRequestException("Refund can only be requested after the order has been delivered.");
        }

        // Rule 2: Refund is only allowed within 7 days of delivery
        if (LocalDateTime.now().isAfter(deliveredAt.plusDays(REFUND_WINDOW_DAYS))) {
            throw new InvalidRequestException(
                    "Refund request period has expired. Refunds can only be requested within " 
                    + REFUND_WINDOW_DAYS + " days of delivery.");
        }

        int reservedQty = refundRequestRepository.getReservedQuantity(orderItem.getOrderItemId());
        int remainingQty = orderItem.getQty() - reservedQty;

        if (dto.getRefundQuantity() > remainingQty) {
            throw new InvalidRequestException("Requested quantity exceeds remaining refundable quantity");
        }

        BigDecimal refundAmount = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(dto.getRefundQuantity()));

        RefundRequest req = new RefundRequest();
        req.setOrderItem(orderItem);
        req.setRefundQuantity(dto.getRefundQuantity());
        req.setRequestReason(dto.getRequestReason());
        req.setRefundAmount(refundAmount);
        req.setStatus("PENDING");
        req.setRequestedAt(LocalDateTime.now());
        
        req = refundRequestRepository.save(req);

        try {
            adminNotificationService.sendRefundRequestNotification(req);
        } catch (Exception ex) {
            // non-blocking notification failure
        }

        return buildResponse(req, remainingQty - req.getRefundQuantity());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponseDto> getMyRefunds(Long customerId) {
        List<RefundRequest> requests = refundRequestRepository.findByOrderItemOrderCustomerCustomerIdOrderByRequestedAtDesc(customerId);
        return requests.stream().map(req -> {
            int reservedQty = refundRequestRepository.getReservedQuantity(req.getOrderItem().getOrderItemId());
            int remainingQty = req.getOrderItem().getQty() - reservedQty;
            return buildResponse(req, remainingQty);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponseDto getRefundById(Long customerId, Long refundId) {
        RefundRequest req = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found"));
                
        if (!req.getOrderItem().getOrder().getCustomer().getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Refund request not found"); // Prevents IDOR
        }
        
        int reservedQty = refundRequestRepository.getReservedQuantity(req.getOrderItem().getOrderItemId());
        int remainingQty = req.getOrderItem().getQty() - reservedQty;
        
        return buildResponse(req, remainingQty);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundEligibilityDto getRefundEligibility(Long customerId, Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found"));
                
        Order order = orderItem.getOrder();
        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("OrderItem not found"); // Prevents IDOR
        }

        int reservedQty = refundRequestRepository.getReservedQuantity(orderItemId);
        int remainingQty = orderItem.getQty() - reservedQty;

        boolean canRequestRefund = true;
        String ineligibleReason = null;
        Integer daysRemaining = null;

        if (!"SUCCESS".equals(order.getPaymentStatus())) {
            canRequestRefund = false;
            ineligibleReason = "Order is not paid.";
        } else {
            // Rule 1: Check if delivered
            LocalDateTime deliveredAt = getDeliveryTimestampIfDelivered(order);
            if (deliveredAt == null) {
                canRequestRefund = false;
                ineligibleReason = "Refund can only be requested after the order has been delivered.";
            } else {
                // Rule 2: Check 7 days limit from delivery
                LocalDateTime expiry = deliveredAt.plusDays(REFUND_WINDOW_DAYS);
                if (LocalDateTime.now().isAfter(expiry)) {
                    canRequestRefund = false;
                    ineligibleReason = "Refund window expired (refunds must be requested within " 
                            + REFUND_WINDOW_DAYS + " days of delivery).";
                    daysRemaining = 0;
                } else {
                    long hours = Duration.between(LocalDateTime.now(), expiry).toHours();
                    daysRemaining = (int) Math.max(1, Math.ceil(hours / 24.0));

                    if (remainingQty <= 0) {
                        canRequestRefund = false;
                        ineligibleReason = "All items have already been refunded or have pending refund requests.";
                    }
                }
            }
        }

        return RefundEligibilityDto.builder()
                .orderItemId(orderItemId)
                .orderedQuantity(orderItem.getQty())
                .alreadyReservedRefundQuantity(reservedQty)
                .remainingRefundableQuantity(remainingQty)
                .canRequestRefund(canRequestRefund)
                .ineligibleReason(ineligibleReason)
                .daysRemaining(daysRemaining)
                .build();
    }

    /**
     * Helper method to determine if an order is delivered and retrieve the delivery timestamp.
     * Returns the delivery LocalDateTime if delivered; returns null if not yet delivered.
     */
    private LocalDateTime getDeliveryTimestampIfDelivered(Order order) {
        boolean orderIsDelivered = "DELIVERED".equalsIgnoreCase(order.getOrderStatus());

        List<Shipment> shipments = shipmentRepository.findByOrderOrderId(order.getOrderId());
        LocalDateTime shipmentDeliveredAt = null;
        boolean hasDeliveredShipment = false;

        if (shipments != null) {
            for (Shipment s : shipments) {
                if ("DELIVERED".equalsIgnoreCase(s.getShipmentStatus())) {
                    hasDeliveredShipment = true;
                    if (s.getDeliveredAt() != null) {
                        if (shipmentDeliveredAt == null || s.getDeliveredAt().isAfter(shipmentDeliveredAt)) {
                            shipmentDeliveredAt = s.getDeliveredAt();
                        }
                    }
                }
            }
        }

        if (!orderIsDelivered && !hasDeliveredShipment) {
            return null; // Not delivered yet
        }

        if (shipmentDeliveredAt != null) {
            return shipmentDeliveredAt;
        }

        List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderOrderIdOrderByChangedAtAsc(order.getOrderId());
        if (history != null) {
            for (OrderStatusHistory h : history) {
                if ("DELIVERED".equalsIgnoreCase(h.getNewStatus()) && h.getChangedAt() != null) {
                    return h.getChangedAt();
                }
            }
        }

        if (order.getModifiedAt() != null) {
            return order.getModifiedAt();
        }
        return order.getCreatedAt();
    }
    
    private RefundResponseDto buildResponse(RefundRequest req, int remainingQty) {
        return RefundResponseDto.builder()
                .refundId(req.getRefundId())
                .orderItemId(req.getOrderItem().getOrderItemId())
                .productName(req.getOrderItem().getProductName())
                .variantAttributes(req.getOrderItem().getVariantAttributes())
                .orderedQuantity(req.getOrderItem().getQty())
                .requestedRefundQuantity(req.getRefundQuantity())
                .remainingRefundableQuantity(remainingQty)
                .requestReason(req.getRequestReason())
                .refundAmount(req.getRefundAmount())
                .status(req.getStatus())
                .requestedAt(req.getRequestedAt().toString())
                .build();
    }
}
