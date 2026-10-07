package com.ojt.ecommerce.backofficeinventory.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponseDto<T> {
	private List<T> content; // လက်ရှိ Page မှာပါမယ့် Data များ (ဥပမာ - Order 10 ခု)
	private int pageNo;
	private int pageSize;
	private long totalElements;
	private int totalPages;
	private boolean last;
}
