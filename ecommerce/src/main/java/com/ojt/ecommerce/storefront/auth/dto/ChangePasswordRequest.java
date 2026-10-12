package com.ojt.ecommerce.storefront.auth.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
	private String currentPassword;
	private String newPassword;
}