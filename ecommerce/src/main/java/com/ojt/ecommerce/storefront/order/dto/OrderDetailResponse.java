package com.ojt.ecommerce.storefront.order.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor public class OrderDetailResponse {
    private Long orderId;
    private String orderNo;
    private String createdAt;
    private String orderStatus;
    private String paymentStatus;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private List<OrderItemSnapshot> items;
    private OrderAddressSnapshot shippingAddress;
    private OrderPaymentSummary payment;
    private List<OrderStatusHistoryDto> statusHistory;
    private List<ShipmentTracking> shipments;
    private String checkoutType;
    private String reservationExpiresAt;
    private Boolean reservationActive;
    private Boolean isExpired;
}