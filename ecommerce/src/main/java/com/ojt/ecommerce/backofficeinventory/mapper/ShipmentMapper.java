package com.ojt.ecommerce.backofficeinventory.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.ojt.ecommerce.backofficeinventory.dto.ShipmentRequestDto;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.Shipment;

@Component
public class ShipmentMapper {
	public Shipment toEntity(ShipmentRequestDto dto, Order order) {
		return Shipment.builder().order(order).courierName(dto.getCourierName()).trackingNumber(dto.getTrackingNumber())
				.shipmentStatus("SHIPPED").shippedAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build();
	}

	public void updateEntity(Shipment existingShipment, ShipmentRequestDto dto) {
		existingShipment.setCourierName((dto.getCourierName()));
		existingShipment.setTrackingNumber(dto.getTrackingNumber());
		existingShipment.setShippedAt(LocalDateTime.now());
	}
}