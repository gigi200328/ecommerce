package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.dto.AdminNotificationDto;
import com.ojt.ecommerce.entity.RefundRequest;

import java.util.List;
import java.util.Map;

public interface AdminNotificationService {

    /**
     * Creates and broadcasts a real-time notification for a newly submitted refund request.
     */
    AdminNotificationDto sendRefundRequestNotification(RefundRequest refundRequest);

    /**
     * Creates and broadcasts a custom notification.
     */
    AdminNotificationDto sendCustomNotification(String type, String title, String message, String link, Long referenceId);

    /**
     * Gets the latest 20 notifications.
     */
    List<AdminNotificationDto> getRecentNotifications();

    /**
     * Gets summary containing unread count and unread notification items.
     */
    Map<String, Object> getUnreadSummary();

    /**
     * Marks a single notification as read.
     */
    void markAsRead(Long notificationId);

    /**
     * Marks all unread notifications as read.
     */
    void markAllAsRead();
}
