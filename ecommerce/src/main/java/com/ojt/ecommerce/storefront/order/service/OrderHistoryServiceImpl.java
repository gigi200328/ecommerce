package com.ojt.ecommerce.storefront.order.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.ojt.ecommerce.storefront.order.dto.PendingOrderExpiryResponse;
import com.ojt.ecommerce.storefront.order.dto.OrderPaymentSummary;
import com.ojt.ecommerce.storefront.order.dto.OrderStatusHistoryDto;
import com.ojt.ecommerce.storefront.order.dto.ShipmentTracking;
import com.ojt.ecommerce.storefront.order.port.ShipmentTrackingPort;
import com.ojt.ecommerce.storefront.payment.repository.G5PaymentRepository;
import com.ojt.ecommerce.storefront.reservation.service.InventoryReservationService;
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
	private final InventoryReservationService reservationService;

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

		Pageable effectivePageable = pageable;
		if (effectivePageable == null || effectivePageable.getSort().isUnsorted()) {
			effectivePageable = PageRequest.of(
					pageable != null ? pageable.getPageNumber() : 0,
					pageable != null ? pageable.getPageSize() : 10,
					Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "orderId")));
		}

		Page<Order> ordersPage = orderRepository.findAll(spec, effectivePageable);

		List<Long> orderIds = ordersPage.getContent().stream().map(Order::getOrderId).collect(Collectors.toList());

		// Batch fetch items and shipments to avoid N+1
		Map<Long, List<OrderItemSummary>> itemsMap = new HashMap<>();
		Map<Long, List<String>> trackingMap = new HashMap<>();

		if (!orderIds.isEmpty()) {
			List<OrderItem> allItems = orderItemRepository.findByOrderOrderIdIn(orderIds);
			for (OrderItem item : allItems) {
				itemsMap.computeIfAbsent(item.getOrder().getOrderId(), k -> new ArrayList<>())
						.add(OrderItemSummary.builder().orderItemId(item.getOrderItemId()).productName(item.getProductName()).qty(item.getQty()).build());
			}

			List<Shipment> allShipments = shipmentRepository.findByOrderOrderIdIn(orderIds);
			for (Shipment s : allShipments) {
				if (s.getTrackingNumber() != null) {
					trackingMap.computeIfAbsent(s.getOrder().getOrderId(), k -> new ArrayList<>())
							.add(s.getTrackingNumber());
				}
			}
		}

		return ordersPage.map(order -> {
			Optional<LocalDateTime> expiryOpt = reservationService.getReservationExpiryForOrder(order.getOrderId());
			String expiresAtIso = expiryOpt
					.map(exp -> exp.atZone(ZoneId.systemDefault()).toInstant().toString())
					.orElse(null);
			boolean reservationActive = reservationService.isReservationActiveForOrder(order.getOrderId());
			boolean hasInitiatedPayment = paymentRepository.existsByOrderOrderId(order.getOrderId());
			boolean isExpired = reservationService.isOrderExpired(order.getOrderId());

			return OrderListResponse.builder().orderId(order.getOrderId())
					.orderNo(order.getOrderNo()).totalAmount(order.getTotalAmount()).orderStatus(order.getOrderStatus())
					.paymentStatus(order.getPaymentStatus()).createdAt(order.getCreatedAt().toString())
					.items(itemsMap.getOrDefault(order.getOrderId(), Collections.emptyList()))
					.trackingNumbers(trackingMap.getOrDefault(order.getOrderId(), Collections.emptyList()))
					.checkoutType(order.getCheckoutType())
					.reservationExpiresAt(expiresAtIso)
					.reservationActive(reservationActive)
					.hasInitiatedPayment(hasInitiatedPayment)
					.isExpired(isExpired)
					.build();
		});
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
						.costPrice(i.getCostPrice()).subtotal(i.getSubtotal()).build())
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
			String pm = payment.getPaymentMethod();
			if (pm != null && (pm.equalsIgnoreCase("MINI_BANKING") || pm.toUpperCase().contains("MINI"))) {
				pm = "EKARAJ  BANK";
			}
			paymentSummary = OrderPaymentSummary.builder().paymentMethod(pm)
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

		Optional<LocalDateTime> expiryOpt = reservationService.getReservationExpiryForOrder(orderId);
		String expiresAtIso = expiryOpt
				.map(exp -> exp.atZone(ZoneId.systemDefault()).toInstant().toString())
				.orElse(null);
		boolean reservationActive = reservationService.isReservationActiveForOrder(orderId);
		boolean isExpired = reservationService.isOrderExpired(orderId);

		return OrderDetailResponse.builder().orderId(order.getOrderId()).orderNo(order.getOrderNo())
				.createdAt(order.getCreatedAt().toString()).orderStatus(order.getOrderStatus())
				.paymentStatus(order.getPaymentStatus()).subtotalAmount(order.getSubtotalAmount())
				.discountAmount(order.getDiscountAmount()).taxAmount(order.getTaxAmount())
				.shippingFee(order.getShippingFee()).totalAmount(order.getTotalAmount()).items(itemSnapshots)
				.shippingAddress(addressSnapshot).payment(paymentSummary).statusHistory(historyDtos)
				.shipments(shipments)
				.checkoutType(order.getCheckoutType())
				.reservationExpiresAt(expiresAtIso)
				.reservationActive(reservationActive)
				.isExpired(isExpired)
				.build();
	}

	@Override
	@Transactional
	public OrderDetailResponse resumePayment(Long customerId, Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));

		if (!order.getCustomer().getCustomerId().equals(customerId)) {
			throw new ResourceNotFoundException("Order not found");
		}

		if ("PAID".equalsIgnoreCase(order.getOrderStatus()) || "SUCCESS".equalsIgnoreCase(order.getPaymentStatus())) {
			throw new InvalidRequestException("Order is already paid");
		}

		if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
			throw new InvalidRequestException("Cancelled order cannot be resumed");
		}

		if (!"PENDING".equalsIgnoreCase(order.getOrderStatus()) || !"PENDING".equalsIgnoreCase(order.getPaymentStatus())) {
			throw new InvalidRequestException("Order is not in a payable state: " + order.getOrderStatus());
		}

		// Final UX Rule 2: Cannot resume if payment has already been initiated (processing)
		if (paymentRepository.existsByOrderOrderId(orderId)) {
			throw new InvalidRequestException("Payment is currently processing for this order. Please wait for the result.");
		}

		// Final UX Rule 6: Cannot resume if reservation is expired (no re-reserve allowed)
		boolean isActive = reservationService.isReservationActiveForOrder(orderId);
		if (!isActive) {
			throw new InvalidRequestException("The order has expired and can no longer be paid.");
		}

		return getOrderById(customerId, orderId);
	}

	@Override
	@Transactional(readOnly = true)
	public PendingOrderExpiryResponse getPendingOrderExpiry(Long customerId) {
		if (customerId == null) {
			return PendingOrderExpiryResponse.builder().pendingCount(0).build();
		}

		List<Order> pendingOrders = orderRepository.findByCustomerCustomerIdAndOrderStatus(customerId, "PENDING");
		if (pendingOrders.isEmpty()) {
			return PendingOrderExpiryResponse.builder().pendingCount(0).build();
		}

		LocalDateTime now = LocalDateTime.now();
		List<OrderExpiryCandidate> candidates = new ArrayList<>();

		for (Order order : pendingOrders) {
			if (!"PENDING".equalsIgnoreCase(order.getPaymentStatus())) {
				continue;
			}
			if (paymentRepository.existsByOrderOrderId(order.getOrderId())) {
				continue; // Payment already initiated / processing
			}
			Optional<LocalDateTime> expiryOpt = reservationService.getReservationExpiryForOrder(order.getOrderId());
			if (expiryOpt.isPresent() && expiryOpt.get().isAfter(now) && reservationService.isReservationActiveForOrder(order.getOrderId())) {
				candidates.add(new OrderExpiryCandidate(order, expiryOpt.get()));
			}
		}

		if (candidates.isEmpty()) {
			return PendingOrderExpiryResponse.builder().pendingCount(0).build();
		}

		candidates.sort(Comparator.comparing(c -> c.expiresAt));

		OrderExpiryCandidate nearest = candidates.get(0);
		String expiresAtIso = nearest.expiresAt.atZone(ZoneId.systemDefault()).toInstant().toString();

		return PendingOrderExpiryResponse.builder()
				.orderId(nearest.order.getOrderId())
				.orderNo(nearest.order.getOrderNo())
				.expiresAt(expiresAtIso)
				.pendingCount(candidates.size())
				.build();
	}

	private static class OrderExpiryCandidate {
		final Order order;
		final LocalDateTime expiresAt;
		OrderExpiryCandidate(Order order, LocalDateTime expiresAt) {
			this.order = order;
			this.expiresAt = expiresAt;
		}
	}
}
