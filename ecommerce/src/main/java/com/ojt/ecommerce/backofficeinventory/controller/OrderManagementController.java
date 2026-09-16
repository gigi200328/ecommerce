package com.ojt.ecommerce.backofficeinventory.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ojt.ecommerce.backofficeinventory.dto.OrderResponseDto;
import com.ojt.ecommerce.backofficeinventory.service.OrderManagementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/backoffice/orders")
@RequiredArgsConstructor
public class OrderManagementController {

	private final OrderManagementService orderService;

	@GetMapping
	public List<OrderResponseDto> getOrders(@RequestParam(required = false) String status) {
		return orderService.getOrders(status);
	}
}