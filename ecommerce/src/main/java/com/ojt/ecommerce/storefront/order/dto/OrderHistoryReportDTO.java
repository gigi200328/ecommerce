package com.ojt.ecommerce.storefront.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Projection DTO representing a single order row in the Customer Order History Report.
 * Maps directly to the result set returned by {@code sp_get_customer_order_history_report}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryReportDTO {
    private Long orderId;
    private String orderNo;
    private LocalDateTime orderDate;
    private String orderDateStr;
    private String orderStatus;
    private String paymentStatus;
    private String paymentMethod;
    private String itemsSummary;
    private Integer totalItemQuantity;
    private BigDecimal subtotalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
}
