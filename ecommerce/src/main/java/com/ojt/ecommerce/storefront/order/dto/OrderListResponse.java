package com.ojt.ecommerce.storefront.order.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor public class OrderListResponse {
    private Long orderId;
    private String orderNo;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private String createdAt;
    private List<OrderItemSummary> items;
    private List<String> trackingNumbers;
    private String checkoutType;
    private String reservationExpiresAt;
    private Boolean reservationActive;
    private Boolean hasInitiatedPayment;
    private Boolean isExpired;
}