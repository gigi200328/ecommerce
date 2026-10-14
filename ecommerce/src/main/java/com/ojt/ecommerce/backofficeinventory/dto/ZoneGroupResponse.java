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
public class ZoneGroupResponse {
	private String zoneName;
	private String zoneCode;
	private String regionOrState;
	private String city;
	private List<String> townships; // မြို့နယ်များကို စုပြီး Array အနေဖြင့် ပြန်ပို့ပေးမည်
	private List<Long> zoneIds; // ပြန်ပြင်သည့်အခါ သုံးရန် သက်ဆိုင်ရာ zone_id များ
	private boolean isCityDefault; // township = null ဖြစ်သော Default Rate ဟုတ်/မဟုတ်
	private BigDecimal shippingFee;
	private Integer estimatedDays;
	private String status;
}
