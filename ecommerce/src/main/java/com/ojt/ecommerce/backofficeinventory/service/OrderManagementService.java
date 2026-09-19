package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.OrderResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.OrderStatusUpdateRequestDto;
import com.ojt.ecommerce.backofficeinventory.mapper.OrderMapper;
import com.ojt.ecommerce.backofficeinventory.repository.OrderRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderStatusHistoryRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.entity.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderManagementService {

	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;
	private final OrderStatusHistoryRepository orderStatusHistoryRepository;

	@Transactional(readOnly = true)
	public List<OrderResponseDto> getOrders(String status) {
		List<Order> orders;
		// Status ပါလာလျှင် Statusအားလုံး ကိုရှာမည်၊ မပါလာလျှင် အားလုံးကို ရှာမည်
		if (status != null && !status.trim().isEmpty()) {
			String upperStatus = status.toUpperCase();

			List<String> validStatuses = Arrays.asList("PENDING", "PAID", "PROCESSING", "SHIPPED", "DELIVERED",
					"CANCELLED");

			if (!validStatuses.contains(upperStatus)) {
				throw new IllegalArgumentException(
						"Invalid order status: '" + status + "'. Allowed values are: " + validStatuses);
			}
			orders = orderRepository.findByOrderStatus(status);
		} else {
			orders = orderRepository.findAll();
		}
		// Mapper ကို အသုံးပြု၍ Entity မှ DTO သို့ ပြောင်းလဲခြင်း
		return orders.stream().map(orderMapper::toDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public OrderResponseDto getOrderByOrderNo(String orderNo) {
		Order order = orderRepository.findByOrderNo(orderNo)
				.orElseThrow(() -> new EntityNotFoundException("Order not found with order number: " + orderNo));
		return orderMapper.toDto(order);
	}

	@Transactional(readOnly = false)
	public OrderResponseDto updateOrderStatus(String orderNo, OrderStatusUpdateRequestDto requestDto) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		User currentUser = userDetails.getUser();

		Order order = orderRepository.findByOrderNo(orderNo)
				.orElseThrow(() -> new EntityNotFoundException("Order not found with order number: " + orderNo));

		String oldStatus = order.getOrderStatus();
		String newStatus = requestDto.getNewStatus().toUpperCase();

		if (oldStatus.equals(newStatus)) {
			throw new IllegalArgumentException("Order is already in " + newStatus + " status.");
		}

		OrderStatusHistory history = OrderStatusHistory.builder().order(order).oldStatus(oldStatus).newStatus(newStatus)
				.remark(requestDto.getRemark()).changedAt(LocalDateTime.now()).changedBy(currentUser).build();

		orderStatusHistoryRepository.save(history);

		order.setOrderStatus(newStatus);
		order.setModifiedAt(LocalDateTime.now());
		order.setModifiedBy(currentUser);

		Order updatedOrder = orderRepository.save(order);

		// ၆။ ပြောင်းလဲသွားတဲ့ အော်ဒါအချက်အလက်ကို DTO ပြောင်းပြီး ပြန်ပေးမည်
		return orderMapper.toDto(updatedOrder);
	}

}