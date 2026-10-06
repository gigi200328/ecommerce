package com.ojt.ecommerce.backofficeinventory.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ojt.ecommerce.backofficeinventory.dto.OrderResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.OrderStatusUpdateRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.PaymentVerificationDto;
import com.ojt.ecommerce.backofficeinventory.dto.ShipmentRequestDto;
import com.ojt.ecommerce.backofficeinventory.service.OrderManagementService;
import com.ojt.ecommerce.enums.CourierName;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/backoffice/orders")
@RequiredArgsConstructor
public class OrderManagementController {

	private final OrderManagementService orderManagementService;

//	@GetMapping
//	public List<OrderResponseDto> getOrders(@RequestParam(required = false) String status) {
//		return orderService.getOrders(status);
//	}

	@GetMapping
	public ResponseEntity<List<OrderResponseDto>> getOrders(
			@RequestParam(value = "status", required = false) String status) {
		List<OrderResponseDto> orders = orderManagementService.getOrders(status);
		return ResponseEntity.ok(orders);
	}

	@GetMapping("/{orderNo}")
	public ResponseEntity<OrderResponseDto> getOrderByOrderNo(@PathVariable String orderNo) {
		OrderResponseDto orderDetail = orderManagementService.getOrderByOrderNo(orderNo);
		return ResponseEntity.ok(orderDetail);
	}

	@PatchMapping("/{orderNo}/status")
	public ResponseEntity<OrderResponseDto> updateOrderStatus(@PathVariable String orderNo,
			@Valid @RequestBody OrderStatusUpdateRequestDto requestDto) {

		OrderResponseDto updatedOrder = orderManagementService.updateOrderStatus(orderNo, requestDto);
		return ResponseEntity.ok(updatedOrder);
	}

	@GetMapping("/{orderNo}/payment-info")
	public ResponseEntity<PaymentVerificationDto> getPaymentVerificationInfo(@PathVariable("orderNo") String orderNo) {

		PaymentVerificationDto paymentInfo = orderManagementService.getPaymentInfoByOrderNo(orderNo);
		return ResponseEntity.ok(paymentInfo);
	}

	@GetMapping("/couriers")
	public ResponseEntity<List<String>> getAllCouriers() {
		List<String> courierList = Arrays.stream(CourierName.values()).map(CourierName::name)
				.collect(Collectors.toList());

		return ResponseEntity.ok(courierList);
	}

	@PostMapping("/{orderNo}/shipments")
	public ResponseEntity<?> createShipment(@PathVariable String orderNo, @RequestBody ShipmentRequestDto requestDto) {
		// Service ကို လှမ်းခေါ်မည်
		orderManagementService.createShipmentAndUpdateStatus(orderNo, requestDto);

		return ResponseEntity.ok()
				.body(Map.of("message", "Shipment details saved and Order status updated to SHIPPED successfully."));
	}

}