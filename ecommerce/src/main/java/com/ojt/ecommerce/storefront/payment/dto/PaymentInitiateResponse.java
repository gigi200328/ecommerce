package com.ojt.ecommerce.storefront.payment.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data @Builder public class PaymentInitiateResponse {
    private Long paymentId;
    private Long orderId;
    private String paymentStatus;
    private BigDecimal amount;
    private String redirectUrl;
}