package com.ojt.ecommerce.storefront.auth.service;

import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.storefront.exception.ConflictException;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.security.JwtUtil;
import com.ojt.ecommerce.storefront.auth.dto.*;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerAuthServiceImpl implements CustomerAuthService {
	private final CustomerRepository customerRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (customerRepository.existsByEmail(request.getEmail())) {
			throw new ConflictException("Email already in use");
		}
		Customer customer = Customer.builder().fullName(request.getFullName()).email(request.getEmail())
				.phone(request.getPhone()).passwordHash(passwordEncoder.encode(request.getPassword())).status("ACTIVE")
				.createdAt(LocalDateTime.now()).build();
		customer = customerRepository.save(customer);
		String token = jwtUtil.generateToken(customer.getEmail(), customer.getCustomerId());
		return AuthResponse.builder().token(token).customerId(customer.getCustomerId()).fullName(customer.getFullName())
				.email(customer.getEmail()).build();
	}

	@Override
	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		Customer customer = customerRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new InvalidRequestException("Invalid email or password"));

		if (!"ACTIVE".equals(customer.getStatus())) {
			throw new InvalidRequestException("Account is not active");
		}

		if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
			throw new InvalidRequestException("Invalid email or password");
		}

		String token = jwtUtil.generateToken(customer.getEmail(), customer.getCustomerId());
		return AuthResponse.builder().token(token).customerId(customer.getCustomerId()).fullName(customer.getFullName())
				.email(customer.getEmail()).build();
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerMeResponse getCurrentCustomer(Long customerId) {
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
		return CustomerMeResponse.builder().customerId(customer.getCustomerId()).fullName(customer.getFullName())
				.email(customer.getEmail()).phone(customer.getPhone()).build();
	}

	@Override
	@Transactional
	public CustomerMeResponse updateCustomerProfile(Long customerId, CustomerProfileUpdateRequest request) {
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
		customer.setFullName(request.getFullName());
		customer.setPhone(request.getPhone());
		customer.setModifiedAt(LocalDateTime.now());
		customer = customerRepository.save(customer);
		return CustomerMeResponse.builder().customerId(customer.getCustomerId()).fullName(customer.getFullName())
				.email(customer.getEmail()).phone(customer.getPhone()).build();
	}

	@Override
	@Transactional
	public void processForgotPassword(ForgotPasswordRequest request) {
		Customer customer = customerRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new ResourceNotFoundException("User not found with provided email"));

		String resetToken = jwtUtil.generateToken(customer.getEmail(), customer.getCustomerId());

		String resetLink = "http://localhost:3000/reset-password?token=" + resetToken;

		System.out.println("Password Reset Link sent to " + customer.getEmail() + ": " + resetLink);
	}

	@Override
	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		String email = jwtUtil.extractUsername(request.getToken());

		Customer customer = customerRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset token"));

		customer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		customer.setModifiedAt(LocalDateTime.now());
		customerRepository.save(customer);
	}
}