package com.ojt.ecommerce.storefront.order.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data @Builder public class OrderPaymentSummary {
    private String paymentMethod;
    private String paymentStatus;
    private String transactionRef;
    private BigDecimal amount;
    private String paidAt;
}