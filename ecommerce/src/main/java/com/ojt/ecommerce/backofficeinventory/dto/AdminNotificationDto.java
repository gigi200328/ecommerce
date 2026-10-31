package com.ojt.ecommerce.backofficeinventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationDto {
    private Long notificationId;
    private String type;
    private String title;
    private String message;
    private String link;
    private Long referenceId;
    private boolean isRead;
    private LocalDateTime createdAt;
    
    // Optional helper fields for refund notifications
    private String orderNo;
    private String customerName;
    private Double amount;
    private String reason;
}
