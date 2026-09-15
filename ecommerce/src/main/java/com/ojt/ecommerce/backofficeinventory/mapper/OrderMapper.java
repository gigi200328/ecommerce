package com.ojt.ecommerce.backofficeinventory.mapper;

import org.springframework.stereotype.Component;

import com.ojt.ecommerce.backofficeinventory.dto.OrderResponseDto;
import com.ojt.ecommerce.entity.Order;

@Component
public class OrderMapper {

	public OrderResponseDto toDto(Order order) {
		return OrderResponseDto.builder().orderId(order.getOrderId()).orderNo(order.getOrderNo())
				.totalAmount(order.getTotalAmount()).orderStatus(order.getOrderStatus())
				.paymentStatus(order.getPaymentStatus()).createdAt(order.getCreatedAt())
				.customerName(order.getCustomer().getFullName()).build();
	}
}