package com.ojt.ecommerce.backofficeinventory.mapper;

import org.springframework.stereotype.Component;

import com.ojt.ecommerce.backofficeinventory.dto.PaymentVerificationDto;
import com.ojt.ecommerce.entity.Payment;

@Component
public class PaymentMapper {

	public PaymentVerificationDto toDto(Payment payment) {
		if (payment == null) {
			return null;
		}

		return PaymentVerificationDto.builder().paymentId(payment.getPaymentId())
				.orderNo(payment.getOrder() != null ? payment.getOrder().getOrderNo() : null)
				.transactionRef(payment.getTransactionRef()).paymentMethod(payment.getPaymentMethod())
				.paymentStatus(payment.getPaymentStatus()).amount(payment.getAmount()).paidAt(payment.getPaidAt())
				.build();
	}
}