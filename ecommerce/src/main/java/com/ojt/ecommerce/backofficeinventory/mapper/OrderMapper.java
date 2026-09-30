package com.ojt.ecommerce.backofficeinventory.mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ojt.ecommerce.backofficeinventory.dto.OrderAddressDto;
import com.ojt.ecommerce.backofficeinventory.dto.OrderItemDto;
import com.ojt.ecommerce.backofficeinventory.dto.OrderResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.OrderStatusHistoryDto;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderAddress;
import com.ojt.ecommerce.entity.OrderItem;
import com.ojt.ecommerce.entity.OrderStatusHistory;

@Component
public class OrderMapper {

	public OrderResponseDto toDto(Order order) {
		return OrderResponseDto.builder().orderId(order.getOrderId()).orderNo(order.getOrderNo())
				.totalAmount(order.getTotalAmount()).orderStatus(order.getOrderStatus())
				.paymentStatus(order.getPaymentStatus()).createdAt(order.getCreatedAt())
				.customerName(order.getCustomer().getFullName())
				.shippingAddress(order.getOrderAddresses() != null && !order.getOrderAddresses().isEmpty()
						? toAddressDto(order.getOrderAddresses().get(0))
						: null)
				.items(order.getOrderItems() != null
						? order.getOrderItems().stream().map(this::toItemDto).collect(Collectors.toList())
						: Collections.emptyList())
				.statusHistories(order.getStatusHistories() != null
						? order.getStatusHistories().stream().map(this::toHistoryDto).collect(Collectors.toList())
						: Collections.emptyList())
				.build();
	}

	private OrderAddressDto toAddressDto(OrderAddress address) {
		if (address == null)
			return null;
		return OrderAddressDto.builder().orderAddressId(address.getOrderAddressId())
				.recipientName(address.getRecipientName()).phoneNumber(address.getPhoneNumber())
				.addressLine1(address.getAddressLine1()).addressLine2(address.getAddressLine2())
				.township(address.getTownship()).city(address.getCity()).regionOrState(address.getRegionOrState())
				.build();
	}

	private OrderItemDto toItemDto(OrderItem item) {
		if (item == null)
			return null;
		return OrderItemDto.builder().orderItemId(item.getOrderItemId()).productName(item.getProductName())
				.qty(item.getQty()).unitPrice(item.getUnitPrice()).discountAmount(item.getDiscountAmount())
				.subtotal(item.getSubtotal())
				.variantId(item.getVariant() != null ? item.getVariant().getVariantId() : null)
				.variantAttributes(item.getVariantAttributes()).build();
	}

	private OrderStatusHistoryDto toHistoryDto(OrderStatusHistory history) {
		if (history == null)
			return null;
		return OrderStatusHistoryDto.builder().historyId(history.getHistoryId()).oldStatus(history.getOldStatus())
				.newStatus(history.getNewStatus()).remark(history.getRemark()).changedAt(history.getChangedAt())
				.changedByName(history.getChangedBy() != null ? history.getChangedBy().getUserName() : null).build();
	}

}