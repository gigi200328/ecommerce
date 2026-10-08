package com.ojt.ecommerce.storefront.order.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderAddress;
import com.ojt.ecommerce.entity.OrderItem;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.entity.Payment;
import com.ojt.ecommerce.entity.Shipment;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderAddressRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderItemRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.order.dto.OrderAddressSnapshot;
import com.ojt.ecommerce.storefront.order.dto.OrderDetailResponse;
import com.ojt.ecommerce.storefront.order.dto.OrderItemSnapshot;
import com.ojt.ecommerce.storefront.order.dto.OrderItemSummary;
import com.ojt.ecommerce.storefront.order.dto.OrderListResponse;
import com.ojt.ecommerce.storefront.order.dto.OrderPaymentSummary;
import com.ojt.ecommerce.storefront.order.dto.OrderStatusHistoryDto;
import com.ojt.ecommerce.storefront.order.dto.ShipmentTracking;
import com.ojt.ecommerce.storefront.order.port.ShipmentTrackingPort;
import com.ojt.ecommerce.storefront.payment.repository.G5PaymentRepository;
import com.ojt.ecommerce.storefront.shipping.repository.G5ShipmentRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderHistoryServiceImpl implements OrderHistoryService {

	private final G5OrderRepository orderRepository;
	private final G5OrderItemRepository orderItemRepository;
	private final G5OrderAddressRepository orderAddressRepository;
	private final G5OrderStatusHistoryRepository orderStatusHistoryRepository;
	private final G5PaymentRepository paymentRepository;
	private final G5ShipmentRepository shipmentRepository;

	private final ShipmentTrackingPort shipmentTrackingPort;

	@Override
	@Transactional(readOnly = true)
	public Page<OrderListResponse> getMyOrders(Long customerId, String startDate, String endDate, String orderStatus,
			String orderNo, Pageable pageable) {
		if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
			try {
				LocalDate start = LocalDate.parse(startDate);
				LocalDate end = LocalDate.parse(endDate);
				if (start.isAfter(end)) {
					throw new InvalidRequestException("startDate cannot be after endDate");
				}
			} catch (DateTimeParseException e) {
				// ignore here, handled in spec
			}
		}

		Specification<Order> spec = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("customer").get("customerId"), customerId));

			if (orderStatus != null && !orderStatus.isEmpty()) {
				predicates.add(cb.equal(root.get("orderStatus"), orderStatus));
			}
			if (orderNo != null && !orderNo.isEmpty()) {
				predicates.add(cb.like(root.get("orderNo"), "%" + orderNo + "%"));
			}
			if (startDate != null && !startDate.isEmpty()) {
				try {
					LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
					predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
				} catch (DateTimeParseException e) {
					// ignore invalid dates
				}
			}
			if (endDate != null && !endDate.isEmpty()) {
				try {
					LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
					predicates.add(cb.lessThan(root.get("createdAt"), end));
				} catch (DateTimeParseException e) {
					// ignore invalid dates
				}
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};

		Page<Order> ordersPage = orderRepository.findAll(spec, pageable);

		List<Long> orderIds = ordersPage.getContent().stream().map(Order::getOrderId).collect(Collectors.toList());

		// Batch fetch items and shipments to avoid N+1
		Map<Long, List<OrderItemSummary>> itemsMap = new HashMap<>();
		Map<Long, List<String>> trackingMap = new HashMap<>();

		if (!orderIds.isEmpty()) {
			List<OrderItem> allItems = orderItemRepository.findByOrderOrderIdIn(orderIds);
			for (OrderItem item : allItems) {
				itemsMap.computeIfAbsent(item.getOrder().getOrderId(), k -> new ArrayList<>())
						.add(OrderItemSummary.builder().productName(item.getProductName()).qty(item.getQty()).build());
			}

			List<Shipment> allShipments = shipmentRepository.findByOrderOrderIdIn(orderIds);
			for (Shipment s : allShipments) {
				if (s.getTrackingNumber() != null) {
					trackingMap.computeIfAbsent(s.getOrder().getOrderId(), k -> new ArrayList<>())
							.add(s.getTrackingNumber());
				}
			}
		}

		return ordersPage.map(order -> OrderListResponse.builder().orderId(order.getOrderId())
				.orderNo(order.getOrderNo()).totalAmount(order.getTotalAmount()).orderStatus(order.getOrderStatus())
				.paymentStatus(order.getPaymentStatus()).createdAt(order.getCreatedAt().toString())
				.items(itemsMap.getOrDefault(order.getOrderId(), Collections.emptyList()))
				.trackingNumbers(trackingMap.getOrDefault(order.getOrderId(), Collections.emptyList())).build());
	}

	@Override
	@Transactional(readOnly = true)
	public OrderDetailResponse getOrderById(Long customerId, Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));

		if (!order.getCustomer().getCustomerId().equals(customerId)) {
			throw new ResourceNotFoundException("Order not found"); // Prevents IDOR and leaking existence
		}

		List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
		List<OrderItemSnapshot> itemSnapshots = items.stream()
				.map(i -> OrderItemSnapshot.builder().orderItemId(i.getOrderItemId()).productName(i.getProductName())
						.variantAttributes(i.getVariantAttributes()).qty(i.getQty()).unitPrice(i.getUnitPrice())
						.discountAmount(i.getDiscountAmount()).subtotal(i.getSubtotal()).build())
				.collect(Collectors.toList());

		OrderAddress addr = orderAddressRepository.findByOrderOrderId(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order address missing"));

		OrderAddressSnapshot addressSnapshot = OrderAddressSnapshot.builder().recipientName(addr.getRecipientName())
				.phoneNumber(addr.getPhoneNumber()).addressLine1(addr.getAddressLine1())
				.addressLine2(addr.getAddressLine2()).township(addr.getTownship()).city(addr.getCity())
				.regionOrState(addr.getRegionOrState()).build();

		Payment payment = paymentRepository.findFirstByOrderOrderIdOrderByCreatedAtDesc(orderId).orElse(null);
		OrderPaymentSummary paymentSummary = null;
		if (payment != null) {
			paymentSummary = OrderPaymentSummary.builder().paymentMethod(payment.getPaymentMethod())
					.paymentStatus(payment.getPaymentStatus()).transactionRef(payment.getTransactionRef())
					.amount(payment.getAmount())
					.paidAt(payment.getPaidAt() != null ? payment.getPaidAt().toString() : null).build();
		}

		List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderOrderIdOrderByChangedAtAsc(orderId);
		List<OrderStatusHistoryDto> historyDtos = history.stream()
				.map(h -> OrderStatusHistoryDto.builder().oldStatus(h.getOldStatus()).newStatus(h.getNewStatus())
						.remark(h.getRemark()).changedAt(h.getChangedAt().toString()).build())
				.collect(Collectors.toList());

		List<ShipmentTracking> shipments = shipmentTrackingPort.getTrackingByOrderId(orderId);

		return OrderDetailResponse.builder().orderId(order.getOrderId()).orderNo(order.getOrderNo())
				.createdAt(order.getCreatedAt().toString()).orderStatus(order.getOrderStatus())
				.paymentStatus(order.getPaymentStatus()).subtotalAmount(order.getSubtotalAmount())
				.discountAmount(order.getDiscountAmount()).taxAmount(order.getTaxAmount())
				.shippingFee(order.getShippingFee()).totalAmount(order.getTotalAmount()).items(itemSnapshots)
				.shippingAddress(addressSnapshot).payment(paymentSummary).statusHistory(historyDtos)
				.shipments(shipments).build();
	}
}
