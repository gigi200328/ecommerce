package com.ojt.ecommerce.storefront.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelResponse {
    private Long orderId;
    private String orderStatus;
    private String paymentStatus;
    private String message;
    private boolean itemsRestored;
}
