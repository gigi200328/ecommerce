package com.ojt.ecommerce.backofficeinventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderStatusUpdateRequestDto {

	@NotBlank(message = "New status is required")
	private String newStatus;

	private String remark;
}