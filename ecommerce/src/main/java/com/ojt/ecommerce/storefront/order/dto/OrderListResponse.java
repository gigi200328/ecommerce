package com.ojt.ecommerce.storefront.order.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data @Builder public class OrderListResponse {
    private Long orderId;
    private String orderNo;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private String createdAt;
    private List<OrderItemSummary> items;
    private List<String> trackingNumbers;
}