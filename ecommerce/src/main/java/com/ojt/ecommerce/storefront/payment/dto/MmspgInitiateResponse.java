package com.ojt.ecommerce.storefront.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder

@NoArgsConstructor
@AllArgsConstructor
public class MmspgInitiateResponse {
  private String paymentMethod;
  private String transactionReference;
  private String paymentUrl;

}