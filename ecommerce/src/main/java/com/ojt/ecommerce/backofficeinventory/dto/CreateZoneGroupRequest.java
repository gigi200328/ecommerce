package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateZoneGroupRequest {
	private String zoneName; // ဥပမာ - "Yangon Downtown"
	private String zoneCode; // ဥပမာ - "YGN-DT"
	private String regionOrState; // ဥပမာ - "Yangon Region"
	private String city; // ဥပမာ - "Yangon"
	private List<String> townships; // ဥပမာ - ["Latha", "Pabedan"] (Default City Rate ဆိုလျှင် အလွတ်/null ပို့မည်)
	private BigDecimal shippingFee; // ဥပမာ - 2000
	private Integer estimatedDays;
}
