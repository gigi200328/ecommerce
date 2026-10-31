package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.dto.AdminNotificationDto;
import com.ojt.ecommerce.backofficeinventory.service.AdminNotificationService;
import com.ojt.ecommerce.entity.RefundRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<AdminNotificationDto>> getRecentNotifications() {
        return ResponseEntity.ok(notificationService.getRecentNotifications());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadSummary() {
        return ResponseEntity.ok(notificationService.getUnreadSummary());
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    /**
     * Testing endpoint to immediately trigger and test real-time WebSocket notification
     * directly from admin side or swagger.
     */
    @PostMapping("/test-refund")
    public ResponseEntity<AdminNotificationDto> triggerTestRefundNotification(
            @RequestParam(value = "orderNo", defaultValue = "ORD-TEST-999") String orderNo,
            @RequestParam(value = "customer", defaultValue = "Aung Aung") String customer,
            @RequestParam(value = "amount", defaultValue = "85.50") Double amount,
            @RequestParam(value = "reason", defaultValue = "DAMAGED_ITEM") String reason) {

        String title = "New Refund Request";
        String message = String.format("%s requested a refund of $%.2f for Order %s (%s)",
                customer, amount, orderNo, reason);

        AdminNotificationDto dto = notificationService.sendCustomNotification(
                "REFUND_REQUEST", title, message, "/admin/refunds", 999L
        );
        dto.setOrderNo(orderNo);
        dto.setCustomerName(customer);
        dto.setAmount(amount);
        dto.setReason(reason);

        return ResponseEntity.ok(dto);
    }
}
