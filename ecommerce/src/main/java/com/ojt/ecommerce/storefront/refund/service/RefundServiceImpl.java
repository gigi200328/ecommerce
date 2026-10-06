package com.ojt.ecommerce.storefront.refund.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderItemRepository;
import com.ojt.ecommerce.storefront.refund.dto.*;
import com.ojt.ecommerce.storefront.refund.repository.RefundRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final G5OrderItemRepository orderItemRepository;
    
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

        boolean canRequestRefund = "SUCCESS".equals(order.getPaymentStatus());
        int reservedQty = refundRequestRepository.getReservedQuantity(orderItemId);
        int remainingQty = orderItem.getQty() - reservedQty;
        
        if (remainingQty <= 0) canRequestRefund = false;

        return RefundEligibilityDto.builder()
                .orderItemId(orderItemId)
                .orderedQuantity(orderItem.getQty())
                .alreadyReservedRefundQuantity(reservedQty)
                .remainingRefundableQuantity(remainingQty)
                .canRequestRefund(canRequestRefund)
                .build();
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
