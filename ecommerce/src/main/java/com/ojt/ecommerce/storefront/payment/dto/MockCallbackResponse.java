package com.ojt.ecommerce.storefront.payment.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class MockCallbackResponse {
    private Long orderId;
    private String paymentStatus;
}