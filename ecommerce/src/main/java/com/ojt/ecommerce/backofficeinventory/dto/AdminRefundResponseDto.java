package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundResponseDto {

    private Long refundId;

    private Long orderItemId;

    private Long orderId;

    private String orderNo;

    private String customerName;

    private String customerEmail;

    private String productName;

    private Integer refundQuantity;

    private BigDecimal refundAmount;

    private String reason;

    private String status;

    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;

    private String processedByName;

    private String adminNote;
}
