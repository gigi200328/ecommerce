package com.ojt.ecommerce.storefront.payment.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentInitiateResponse {
  private Long paymentId;
  private Long orderId;
  private String paymentStatus;
  private BigDecimal amount;
  private String redirectUrl;
}