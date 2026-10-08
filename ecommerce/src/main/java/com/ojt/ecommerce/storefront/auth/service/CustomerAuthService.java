package com.ojt.ecommerce.storefront.auth.service;

import com.ojt.ecommerce.storefront.auth.dto.AuthResponse;
import com.ojt.ecommerce.storefront.auth.dto.ChangePasswordRequest;
import com.ojt.ecommerce.storefront.auth.dto.CustomerMeResponse;
import com.ojt.ecommerce.storefront.auth.dto.CustomerProfileUpdateRequest;
import com.ojt.ecommerce.storefront.auth.dto.ForgotPasswordRequest;
import com.ojt.ecommerce.storefront.auth.dto.LoginRequest;
import com.ojt.ecommerce.storefront.auth.dto.RegisterRequest;
import com.ojt.ecommerce.storefront.auth.dto.ResetPasswordRequest;
import com.ojt.ecommerce.storefront.auth.dto.VerifyCodeRequest;

public interface CustomerAuthService {
	AuthResponse login(LoginRequest request);

	AuthResponse register(RegisterRequest request);

	CustomerMeResponse getCurrentCustomer(Long customerId);

	CustomerMeResponse updateCustomerProfile(Long customerId, CustomerProfileUpdateRequest request);

	void processForgotPassword(ForgotPasswordRequest request);

	void verifyResetCode(VerifyCodeRequest request);

	void resetPassword(ResetPasswordRequest request);

	void changePassword(Long customerId, ChangePasswordRequest request);
}