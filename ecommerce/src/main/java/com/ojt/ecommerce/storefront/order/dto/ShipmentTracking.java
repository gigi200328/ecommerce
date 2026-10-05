package com.ojt.ecommerce.storefront.order.dto;

import com.ojt.ecommerce.enums.CourierName;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentTracking {
	private CourierName courierName;
	private String trackingNumber;
	private String shipmentStatus;
	private String shippedAt;
	private String deliveredAt;
}