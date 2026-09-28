package com.ojt.ecommerce.storefront.order.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.entity.Shipment;
import com.ojt.ecommerce.storefront.order.dto.ShipmentTracking;
import com.ojt.ecommerce.storefront.order.port.ShipmentTrackingPort;
import com.ojt.ecommerce.storefront.shipping.repository.G5ShipmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalShipmentTrackingService implements ShipmentTrackingPort {
	private final G5ShipmentRepository shipmentRepository;

	@Override
	@Transactional(readOnly = true)
	public List<ShipmentTracking> getTrackingByOrderId(Long orderId) {
		List<Shipment> shipments = shipmentRepository.findByOrderOrderId(orderId);
		return shipments.stream()
				.map(s -> ShipmentTracking.builder().courierName(s.getCourierName())
						.trackingNumber(s.getTrackingNumber()).shipmentStatus(s.getShipmentStatus())
						.shippedAt(s.getShippedAt() != null ? s.getShippedAt().toString() : null)
						.deliveredAt(s.getDeliveredAt() != null ? s.getDeliveredAt().toString() : null).build())
				.collect(Collectors.toList());
	}
}
