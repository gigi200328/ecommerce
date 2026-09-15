package com.ojt.ecommerce.backofficeinventory.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ojt.ecommerce.backofficeinventory.dto.OrderResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.OrderMapper;
import com.ojt.ecommerce.backofficeinventory.repository.OrderRepository;
import com.ojt.ecommerce.entity.Order;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderManagementService {

	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;

	public List<OrderResponseDto> getOrders(String status) {
		List<Order> orders;
		// Status ပါလာလျှင် 해당 Status ကိုရှာမည်၊ မပါလာလျှင် အားလုံးကို ရှာမည်
		if (status != null && !status.trim().isEmpty()) {
			orders = orderRepository.findByOrderStatus(status);
		} else {
			orders = orderRepository.findAll();
		}
		// Mapper ကို အသုံးပြု၍ Entity မှ DTO သို့ ပြောင်းလဲခြင်း
		return orders.stream().map(orderMapper::toDto).collect(Collectors.toList());
	}
}