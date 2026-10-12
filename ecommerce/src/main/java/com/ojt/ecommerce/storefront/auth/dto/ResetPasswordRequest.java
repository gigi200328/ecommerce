package com.ojt.ecommerce.storefront.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	private String email;

	@NotBlank(message = "Verification code is required")
	@Size(min = 6, max = 6, message = "Code must be 6 digits")
	private String code;

	@NotBlank(message = "New password is required")
	@Size(min = 6, message = "Password must be at least 6 characters")
	private String newPassword;
}