package com.ojt.ecommerce.backofficeinventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ojt.ecommerce.backofficeinventory.dto.AdminNotificationDto;
import com.ojt.ecommerce.backofficeinventory.repository.AdminNotificationRepository;
import com.ojt.ecommerce.config.websocket.NotificationWebSocketHandler;
import com.ojt.ecommerce.entity.AdminNotification;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderItem;
import com.ojt.ecommerce.entity.RefundRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final AdminNotificationRepository notificationRepository;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    @Transactional
    public AdminNotificationDto sendRefundRequestNotification(RefundRequest refundRequest) {
        String orderNo = "N/A";
        String customerName = "Customer";
        Double amount = 0.0;
        String reason = refundRequest.getRequestReason() != null ? refundRequest.getRequestReason() : "Refund requested";

        if (refundRequest.getRefundAmount() != null) {
            amount = refundRequest.getRefundAmount().doubleValue();
        }

        OrderItem orderItem = refundRequest.getOrderItem();
        if (orderItem != null) {
            Order order = orderItem.getOrder();
            if (order != null) {
                if (order.getOrderNo() != null && !order.getOrderNo().isBlank()) {
                    orderNo = order.getOrderNo();
                } else if (order.getOrderId() != null) {
                    orderNo = "#" + order.getOrderId();
                }

                if (order.getCustomer() != null && order.getCustomer().getFullName() != null) {
                    customerName = order.getCustomer().getFullName();
                }
            }
        }

        String title = "New Refund Request";
        String message = String.format("%s requested a refund of $%.2f for Order %s (%s)",
                customerName, amount, orderNo, reason);

        AdminNotification entity = AdminNotification.builder()
                .type("REFUND_REQUEST")
                .title(title)
                .message(message)
                .link("/admin/refunds")
                .referenceId(refundRequest.getRefundId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        entity = notificationRepository.save(entity);

        AdminNotificationDto dto = toDto(entity);
        dto.setOrderNo(orderNo);
        dto.setCustomerName(customerName);
        dto.setAmount(amount);
        dto.setReason(reason);

        // Broadcast over WebSocket to active admin browsers
        broadcastDto(dto);

        return dto;
    }

    @Override
    @Transactional
    public AdminNotificationDto sendCustomNotification(String type, String title, String message, String link, Long referenceId) {
        AdminNotification entity = AdminNotification.builder()
                .type(type != null ? type : "INFO")
                .title(title)
                .message(message)
                .link(link != null ? link : "/admin/refunds")
                .referenceId(referenceId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        entity = notificationRepository.save(entity);
        AdminNotificationDto dto = toDto(entity);
        broadcastDto(dto);
        return dto;
    }

    private void broadcastDto(AdminNotificationDto dto) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "NOTIFICATION");
            event.put("data", dto);
            event.put("unreadCount", notificationRepository.countByIsReadFalse());

            String json = objectMapper.writeValueAsString(event);
            webSocketHandler.broadcast(json);
            log.info("Broadcasted notification #{} via WebSocket to {} connected sessions",
                    dto.getNotificationId(), webSocketHandler.getActiveSessionCount());
        } catch (Exception e) {
            log.error("Failed to serialize or broadcast notification", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminNotificationDto> getRecentNotifications() {
        return notificationRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadSummary() {
        long unreadCount = notificationRepository.countByIsReadFalse();
        List<AdminNotificationDto> unreadList = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", unreadCount);
        result.put("unreadNotifications", unreadList);
        return result;
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsRead();
    }

    private AdminNotificationDto toDto(AdminNotification entity) {
        return AdminNotificationDto.builder()
                .notificationId(entity.getNotificationId())
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .link(entity.getLink())
                .referenceId(entity.getReferenceId())
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
