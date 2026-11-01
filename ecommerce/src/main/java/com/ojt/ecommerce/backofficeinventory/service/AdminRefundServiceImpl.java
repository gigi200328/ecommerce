package com.ojt.ecommerce.backofficeinventory.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.AdminRefundResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.RefundDecisionDto;
import com.ojt.ecommerce.backofficeinventory.dto.RefundStatsDto;
import com.ojt.ecommerce.backofficeinventory.repository.AdminRefundRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderStatusHistoryRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.entity.RefundRequest;
import com.ojt.ecommerce.entity.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("adminRefundService")
@RequiredArgsConstructor
public class AdminRefundServiceImpl implements AdminRefundService {

    private final AdminRefundRepository adminRefundRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public List<AdminRefundResponseDto> getRefunds(String search, String status, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        List<RefundRequest> entities = adminRefundRepository.findRefundsWithFilters(search, status, startDateTime, endDateTime);
        return entities.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RefundStatsDto getRefundStats() {
        long total = adminRefundRepository.countTotalRequests();
        long pending = adminRefundRepository.countPendingRequests();
        long approved = adminRefundRepository.countApprovedRequests();
        long rejected = adminRefundRepository.countRejectedRequests();
        BigDecimal totalRefunded = adminRefundRepository.sumApprovedRefundAmount();
        if (totalRefunded == null) {
            totalRefunded = BigDecimal.ZERO;
        }

        return RefundStatsDto.builder()
                .totalRequests(total)
                .pendingCount(pending)
                .approvedCount(approved)
                .rejectedCount(rejected)
                .totalRefundedAmount(totalRefunded)
                .build();
    }

    @Override
    @Transactional
    public AdminRefundResponseDto approveRefund(Long refundId, RefundDecisionDto request) {
        RefundRequest refund = adminRefundRepository.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("Refund request not found with ID: " + refundId));

        if (!"PENDING".equalsIgnoreCase(refund.getStatus())) {
            throw new IllegalStateException("Only PENDING refund requests can be approved. Current status: " + refund.getStatus());
        }

        refund.setStatus("APPROVED");
        if (request != null && request.getAdminNote() != null && !request.getAdminNote().isBlank()) {
            refund.setRemark(request.getAdminNote().trim());
        }
        refund.setProcessedAt(LocalDateTime.now());

        User currentUser = getCurrentUser();
        if (currentUser != null) {
            refund.setProcessedBy(currentUser);
        }

        // Also update the linked Order in the orders table (payment_status = 'REFUNDED')
        if (refund.getOrderItem() != null && refund.getOrderItem().getOrder() != null) {
            Order order = refund.getOrderItem().getOrder();
            order.setPaymentStatus("REFUNDED");
            order.setModifiedAt(LocalDateTime.now());
            if (currentUser != null) {
                order.setModifiedBy(currentUser);
            }
            orderRepository.save(order);

            // Audit status tracking
            OrderStatusHistory history = OrderStatusHistory.builder()
                    .order(order)
                    .oldStatus(order.getOrderStatus())
                    .newStatus(order.getOrderStatus())
                    .remark("Order payment status updated to REFUNDED following approval of Refund Request #" + refundId)
                    .changedAt(LocalDateTime.now())
                    .changedBy(currentUser)
                    .build();
            orderStatusHistoryRepository.save(history);
        }

        // 🔴 Member 3 Integration: Restock returned items to Inventory and log RETURN transaction
        if (refund.getOrderItem() != null && refund.getOrderItem().getVariant() != null) {
            Long variantId = refund.getOrderItem().getVariant().getVariantId();
            int returnQty = refund.getRefundQuantity() != null ? refund.getRefundQuantity() : 1;
            String note = (request != null && request.getAdminNote() != null && !request.getAdminNote().isBlank())
                    ? request.getAdminNote().trim()
                    : ("Restocked returned item from approved Refund #" + refundId);
            inventoryService.restoreStockForRefund(variantId, returnQty, refundId, note);
        }

        RefundRequest saved = adminRefundRepository.save(refund);
        log.info("Refund request #{} successfully APPROVED by user: {}", refundId, currentUser != null ? currentUser.getEmail() : "SYSTEM");
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public AdminRefundResponseDto rejectRefund(Long refundId, RefundDecisionDto request) {
        RefundRequest refund = adminRefundRepository.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("Refund request not found with ID: " + refundId));

        if (!"PENDING".equalsIgnoreCase(refund.getStatus())) {
            throw new IllegalStateException("Only PENDING refund requests can be rejected. Current status: " + refund.getStatus());
        }

        String note = (request != null && request.getAdminNote() != null && !request.getAdminNote().isBlank()) 
                ? request.getAdminNote().trim()
                : (request != null && request.getReason() != null ? request.getReason().trim() : null);

        if (note == null || note.isEmpty()) {
            throw new IllegalArgumentException("Rejection note / reason is required to reject a refund request.");
        }

        refund.setStatus("REJECTED");
        refund.setRemark(note);
        refund.setProcessedAt(LocalDateTime.now());

        User currentUser = getCurrentUser();
        if (currentUser != null) {
            refund.setProcessedBy(currentUser);
        }

        RefundRequest saved = adminRefundRepository.save(refund);
        log.info("Refund request #{} successfully REJECTED. Reason: {}", refundId, note);
        return mapToDto(saved);
    }

    private AdminRefundResponseDto mapToDto(RefundRequest entity) {
        String orderNo = null;
        Long orderId = null;
        Long orderItemId = null;
        String customerName = null;
        String customerEmail = null;
        String productName = null;

        if (entity.getOrderItem() != null) {
            orderItemId = entity.getOrderItem().getOrderItemId();
            productName = entity.getOrderItem().getProductName();
            if (entity.getOrderItem().getOrder() != null) {
                Order o = entity.getOrderItem().getOrder();
                orderId = o.getOrderId();
                orderNo = o.getOrderNo();
                if (o.getCustomer() != null) {
                    customerName = o.getCustomer().getFullName();
                    customerEmail = o.getCustomer().getEmail();
                }
            }
        }

        String processedByName = null;
        if (entity.getProcessedBy() != null) {
            processedByName = entity.getProcessedBy().getUserName() != null ? 
                    entity.getProcessedBy().getUserName() : entity.getProcessedBy().getEmail();
        }

        return AdminRefundResponseDto.builder()
                .refundId(entity.getRefundId())
                .orderItemId(orderItemId)
                .orderId(orderId)
                .orderNo(orderNo)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .productName(productName)
                .refundQuantity(entity.getRefundQuantity())
                .refundAmount(entity.getRefundAmount())
                .reason(entity.getRequestReason())
                .status(entity.getStatus())
                .requestedAt(entity.getRequestedAt())
                .processedAt(entity.getProcessedAt())
                .processedByName(processedByName)
                .adminNote(entity.getRemark())
                .build();
    }

    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                return ((CustomUserDetails) authentication.getPrincipal()).getUser();
            }
        } catch (Exception e) {
            log.debug("No authenticated user found in SecurityContext");
        }
        return null;
    }
}
