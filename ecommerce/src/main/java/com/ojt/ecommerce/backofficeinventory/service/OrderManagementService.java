package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.OrderResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.OrderStatusHistoryDto;
import com.ojt.ecommerce.backofficeinventory.dto.OrderStatusUpdateRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.PaginatedResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.PaymentVerificationDto;
import com.ojt.ecommerce.backofficeinventory.dto.ShipmentRequestDto;
import com.ojt.ecommerce.backofficeinventory.mapper.OrderMapper;
import com.ojt.ecommerce.backofficeinventory.mapper.PaymentMapper;
import com.ojt.ecommerce.backofficeinventory.mapper.ShipmentMapper;
import com.ojt.ecommerce.backofficeinventory.repository.OrderRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderStatusHistoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.PaymentRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ShipmentRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.entity.Payment;
import com.ojt.ecommerce.entity.Shipment;
import com.ojt.ecommerce.entity.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderManagementService {

	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;
	private final OrderStatusHistoryRepository orderStatusHistoryRepository;
	private final PaymentRepository paymentRepository;
	private final ShipmentRepository shipmentRepository;
	private final PaymentMapper paymentMapper;
	private final ShipmentMapper shipmentMapper;
	private final InventoryService inventoryService;

	@Transactional(readOnly = true)
	public PaginatedResponseDto<OrderResponseDto> getOrders(String status, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
		Page<Order> orders;
		// Status ပါလာလျှင် Statusအားလုံး ကိုရှာမည်၊ မပါလာလျှင် အားလုံးကို ရှာမည်
		if (status != null && !status.trim().isEmpty()) {
			String upperStatus = status.toUpperCase();

			List<String> validStatuses = Arrays.asList("PENDING", "PAID", "PROCESSING", "SHIPPED", "DELIVERED",
					"CANCELLED");

			if (!validStatuses.contains(upperStatus)) {
				throw new IllegalArgumentException(
						"Invalid order status: '" + status + "'. Allowed values are: " + validStatuses);
			}
			orders = orderRepository.findByOrderStatus(status, pageable);
		} else {
			orders = orderRepository.findAll(pageable);
		}
		// Mapper ကို အသုံးပြု၍ Entity မှ DTO သို့ ပြောင်းလဲခြင်း
		return orderMapper.toPaginatedDto(orders);
	}

	@Transactional(readOnly = true)
	public OrderResponseDto getOrderByOrderNo(String orderNo) {
		Order order = orderRepository.findByOrderNo(orderNo)
				.orElseThrow(() -> new EntityNotFoundException("Order not found with order number: " + orderNo));
		return orderMapper.toDto(order);
	}

	private static final Map<String, List<String>> VALID_TRANSITIONS = Map.of("PENDING", List.of("PAID"), "PAID",
			List.of("PROCESSING"), "PROCESSING", List.of("SHIPPED"), "SHIPPED", List.of("DELIVERED"), "DELIVERED",
			List.of("REFUNDED") // ⬅️ DELIVERED ရောက်လျှင် အခြားသို့ နောက်ပြန်ဆုတ်ခွင့် မရှိပါ
	);

	@Transactional(readOnly = false)
	public OrderResponseDto updateOrderStatus(String orderNo, OrderStatusUpdateRequestDto requestDto) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		User currentUser = userDetails.getUser();

		Order order = orderRepository.findByOrderNo(orderNo)
				.orElseThrow(() -> new EntityNotFoundException("Order not found with order number: " + orderNo));

		String oldStatus = order.getOrderStatus();
		String newStatus = requestDto.getNewStatus();

		if (oldStatus.equals(newStatus)) {
			throw new IllegalArgumentException("Order is already in " + newStatus + " status.");
		}

		List<String> allowedNextStatuses = VALID_TRANSITIONS.getOrDefault(oldStatus, Collections.emptyList());
		if (!allowedNextStatuses.contains(newStatus)) {
			throw new IllegalArgumentException(
					"Invalid transition: Cannot change status from " + oldStatus + " to " + newStatus);
		}

		OrderStatusHistory history = OrderStatusHistory.builder().order(order).oldStatus(oldStatus).newStatus(newStatus)
				.remark(requestDto.getRemark()).changedAt(LocalDateTime.now()).changedBy(currentUser).build();

		orderStatusHistoryRepository.save(history);

		order.setOrderStatus(newStatus);
		order.setModifiedAt(LocalDateTime.now());
		order.setModifiedBy(currentUser);

		Order updatedOrder = orderRepository.save(order);

		// 🔴 FR-8: Deduct inventory stock if moving to PAID, PROCESSING, or SHIPPED
		if ("PAID".equalsIgnoreCase(newStatus) || "PROCESSING".equalsIgnoreCase(newStatus)
				|| "SHIPPED".equalsIgnoreCase(newStatus)) {
			inventoryService.deductStockForOrder(updatedOrder.getOrderId());
		}

		// ပြောင်းလဲသွားတဲ့ အော်ဒါအချက်အလက်ကို DTO ပြောင်းပြီး ပြန်ပေးမည်
		return orderMapper.toDto(updatedOrder);
	}

	@Transactional(readOnly = true)
	public PaymentVerificationDto getPaymentInfoByOrderNo(String orderNo) {
		Payment payment = paymentRepository.findByOrder_OrderNo(orderNo).orElseThrow(
				() -> new EntityNotFoundException("Payment information not found for order number: " + orderNo));
		return paymentMapper.toDto(payment);
	}

	@Transactional
	public void createShipmentAndUpdateStatus(String orderNo, ShipmentRequestDto requestDto) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		User currentUser = userDetails.getUser();

		// ၁။ Order ကို ရှာမည်
		Order order = orderRepository.findByOrderNo(orderNo)
				.orElseThrow(() -> new EntityNotFoundException("Order not found with orderNo: " + orderNo));

		String oldStatus = order.getOrderStatus();
		String newStatus = "SHIPPED";

		if (order.getShipment() != null) {
			shipmentMapper.updateEntity(order.getShipment(), requestDto);
			shipmentRepository.save(order.getShipment());
		} else {
			Shipment newShipment = shipmentMapper.toEntity(requestDto, order);
			shipmentRepository.save(newShipment);
		}

		OrderStatusHistory history = OrderStatusHistory.builder().order(order).oldStatus(oldStatus).newStatus(newStatus)
				// Courier Service နာမည်ကိုပါ Remark မှာ ထည့်ပေးထားပါတယ်
				.remark("Order Shipped via " + requestDto.getCourierName()).changedAt(LocalDateTime.now())
				.changedBy(currentUser) // Current User ကို ထည့်ပေးလိုက်ပါပြီ
				.build();

		orderStatusHistoryRepository.save(history);

		// ၅။ Order ၏ Status ကို ပြောင်းပြီး သိမ်းမည်
		order.setOrderStatus(newStatus);
		order.setModifiedAt(LocalDateTime.now());
		order.setModifiedBy(currentUser);

		orderRepository.save(order);

		// 🔴 FR-8: Ensure stock is deducted before shipment dispatch
		inventoryService.deductStockForOrder(order.getOrderId());
	}

	@Transactional(readOnly = true)
	public List<OrderStatusHistoryDto> getOrderStatusHistory(String orderNo) {

		// ၁။ Order No ဖြင့် Order ကို ရှာမည်
		Order order = orderRepository.findByOrderNo(orderNo)
				.orElseThrow(() -> new IllegalArgumentException("Order not found with orderNo: " + orderNo));

		// ၂။ Order ID ဖြင့် History များကို ဆွဲထုတ်မည်
		List<OrderStatusHistory> histories = orderStatusHistoryRepository
				.findByOrder_OrderIdOrderByChangedAtDesc(order.getOrderId());

		// ၃။ Mapper အသုံးပြု၍ DTO သို့ ပြောင်းမည် (Code အလွန်ရှင်းသွားပါပြီ)
		return histories.stream().map(orderMapper::toHistoryDto).collect(Collectors.toList());
	}

}
