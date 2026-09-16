package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Getter, Setter များကို အလိုအလျောက် ထုတ်ပေးမည်
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
	private Long orderId; // Entity ပါအတိုင်း
	private String orderNo; // Entity ပါအတိုင်း
	// Customer Entity ထဲကနေ Frontend ကို နာမည် သို့မဟုတ် Email လေးပဲ ပို့ပေးရန်
	private String customerName;
	private BigDecimal totalAmount; // Entity ပါအတိုင်း
	private String orderStatus; // Entity ပါအတိုင်း (PENDING, PROCESSING, SHIPPED, etc.)
	private String paymentStatus; // Entity ပါအတိုင်း (PENDING, SUCCESS, FAILED, etc.)
	private LocalDateTime createdAt; // Entity ပါအတိုင်း
	// အော်ဒါအသေးစိတ် ကြည့်သည့်အခါ ပြရန် ပစ္စည်းစာရင်း
	private List<OrderItemDto> items;
	// အော်ဒါ၏ အဆင့်ဆင့် ပြောင်းလဲခဲ့မှု သမိုင်းကြောင်းများ (Tracking)
	private List<OrderStatusHistoryDto> statusHistories;
}
