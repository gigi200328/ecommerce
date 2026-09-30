package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
	private Long orderItemId; // Entity ပါအတိုင်း
	// Variant ရဲ့ ID လေးကိုပဲ ဆွဲထုတ်ပေးပါမယ် (Product Variant)
	private Long variantId;
	private String productName; // Entity ပါအတိုင်း
	private String variantAttributes; // ဥပမာ - {"Color": "Red", "Size": "M"}
	private Integer qty; // Entity ပါအတိုင်း (quantity အစား qty)
	private BigDecimal unitPrice; // Entity ပါအတိုင်း
	private BigDecimal discountAmount; // Entity ပါအတိုင်း
	private BigDecimal subtotal; // Entity ပါအတိုင်း
}