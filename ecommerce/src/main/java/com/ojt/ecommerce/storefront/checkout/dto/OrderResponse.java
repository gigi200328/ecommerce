package com.ojt.ecommerce.storefront.checkout.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data @Builder public class OrderResponse {
    private Long orderId;
    private String orderNo;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private String createdAt;
}