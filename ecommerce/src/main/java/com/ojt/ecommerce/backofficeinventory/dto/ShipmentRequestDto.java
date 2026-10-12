package com.ojt.ecommerce.backofficeinventory.dto;

import com.ojt.ecommerce.enums.CourierName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentRequestDto {
	private CourierName courierName;
	private String trackingNumber;
}
