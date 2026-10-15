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
public class UpdateZoneRateRequest {
	private List<Long> zoneIds; // ပြင်ဆင်မည့် Zone Group ထဲရှိ zone_id များအားလုံး
	private String zoneName; // Zone အမည်ပြင်ချင်ပါက သုံးရန်
	private BigDecimal shippingFee; // ဈေးနှုန်းအသစ်
	private Integer estimatedDays; // ကြာချိန်အသစ်
	private String status;
}
