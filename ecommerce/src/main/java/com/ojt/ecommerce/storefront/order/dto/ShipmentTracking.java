package com.ojt.ecommerce.storefront.order.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentTracking {
	private String courierName;
	private String trackingNumber;
	private String shipmentStatus;
	private String shippedAt;
	private String deliveredAt;
}