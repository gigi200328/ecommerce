package com.ojt.ecommerce.backofficeinventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAddressDto {
	private Long orderAddressId;
	private String recipientName;
	private String phoneNumber;
	private String addressLine1;
	private String addressLine2;
	private String township;
	private String city;
	private String regionOrState;
}
