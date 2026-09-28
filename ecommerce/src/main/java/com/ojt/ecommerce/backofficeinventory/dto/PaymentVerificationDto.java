package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerificationDto {
	private Long paymentId;
	private String orderNo;
	private String transactionRef;
	private String paymentMethod;
	private String paymentStatus;
	private BigDecimal amount;
	private LocalDateTime paidAt;
}