package com.ojt.ecommerce.storefront.payment.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data @Builder public class PaymentDetailResponse {
    private Long paymentId;
    private Long orderId;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionRef;
    private BigDecimal amount;
    private String paidAt;
    private String createdAt;
}