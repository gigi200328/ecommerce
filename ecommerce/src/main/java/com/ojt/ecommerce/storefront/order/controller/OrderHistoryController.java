package com.ojt.ecommerce.storefront.order.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ojt.ecommerce.storefront.order.dto.OrderDetailResponse;
import com.ojt.ecommerce.storefront.order.dto.OrderListResponse;
import com.ojt.ecommerce.storefront.order.service.OrderHistoryService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderHistoryController {

	private final OrderHistoryService orderHistoryService;

	@GetMapping
	public ResponseEntity<Page<OrderListResponse>> getMyOrders(@AuthenticationPrincipal CustomerPrincipal principal,
			@RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
			@RequestParam(required = false) String orderStatus, @RequestParam(required = false) String orderNo,
			Pageable pageable) {

		return ResponseEntity.ok(orderHistoryService.getMyOrders(principal.getCustomerId(), startDate, endDate,
				orderStatus, orderNo, pageable));
	}

	@GetMapping("/{orderId}")
	public ResponseEntity<OrderDetailResponse> getOrderById(@AuthenticationPrincipal CustomerPrincipal principal,
			@PathVariable Long orderId) {

		return ResponseEntity.ok(orderHistoryService.getOrderById(principal.getCustomerId(), orderId));
	}
}
