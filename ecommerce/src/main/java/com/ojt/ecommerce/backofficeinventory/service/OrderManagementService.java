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
		String newStatus = requestDto.getNewStatus();

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

	@Transactional(readOnly = true)
	public PaymentVerificationDto getPaymentInfoByOrderNo(String orderNo) {
		Payment payment = paymentRepository.findByOrder_OrderNo(orderNo).orElseThrow(
				() -> new EntityNotFoundException("Payment information not found for order number: " + orderNo));
		return paymentMapper.toDto(payment);
	}

	@Transactional
	public void createShipmentAndUpdateStatus(String orderNo, ShipmentRequestDto requestDto) {
		// ၁။ Order ကို ရှာမည်
		Order order = orderRepository.findByOrderNo(orderNo)
				.orElseThrow(() -> new EntityNotFoundException("Order not found with orderNo: " + orderNo));

		Shipment shipment = shipmentMapper.toEntity(requestDto, order);
		shipmentRepository.save(shipment);

		// ၃။ Order ၏ Status ကို SHIPPED အဖြစ် ပြောင်းပြီး သိမ်းမည်
		order.setOrderStatus("SHIPPED");
		orderRepository.save(order);
	}

}