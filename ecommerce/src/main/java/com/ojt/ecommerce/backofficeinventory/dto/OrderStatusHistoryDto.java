package com.ojt.ecommerce.backofficeinventory.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryDto {
	private Long historyId; // Entity ပါအတိုင်း
	private String oldStatus; // Entity ပါအတိုင်း
	private String newStatus; // Entity ပါအတိုင်း
	private String remark; // Entity ပါအတိုင်း
	private LocalDateTime changedAt; // Entity ပါအတိုင်း
	// User Entity ကြီးတစ်ခုလုံး မပို့ဘဲ Admin နာမည် (သို့) ID လေးပဲ ပို့ပေးရန်
	private String changedByName;
}