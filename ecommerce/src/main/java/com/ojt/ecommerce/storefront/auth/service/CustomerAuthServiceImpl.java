package com.ojt.ecommerce.storefront.auth.service;

import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.storefront.exception.ConflictException;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.security.JwtUtil;
import com.ojt.ecommerce.storefront.auth.dto.*;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerAuthServiceImpl implements CustomerAuthService {

	private final CustomerRepository customerRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final JavaMailSender mailSender;

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String cleanEmail = request.getEmail().trim().toLowerCase();

		if (customerRepository.existsByEmail(cleanEmail)) {
			throw new ConflictException("Email already in use");
		}
		Customer customer = Customer.builder().fullName(request.getFullName()).email(cleanEmail)
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
		String cleanEmail = request.getEmail().trim().toLowerCase();

		Customer customer = customerRepository.findByEmailIgnoreCase(cleanEmail)
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
		String cleanEmail = request.getEmail().trim().toLowerCase();

		Customer customer = customerRepository.findByEmailIgnoreCase(cleanEmail)
				.orElseThrow(() -> new ResourceNotFoundException("ဤ အီးမေးလ်ဖြင့် Register ပြုလုပ်ထားခြင်း မရှိပါ။"));

		SecureRandom random = new SecureRandom();
		String otpCode = String.format("%06d", random.nextInt(1000000));

		customer.setResetPasswordCode(otpCode);
		customer.setResetCodeExpiry(LocalDateTime.now().plusMinutes(5));
		customerRepository.save(customer);

		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(customer.getEmail());
		message.setSubject("Password Reset Code - 6SYNC Store");
		message.setText("Hi " + customer.getFullName() + ",\n\n" + "Your Password Reset Code is: " + otpCode + "\n\n"
				+ "This code will expire in 5 minutes.\n" + "If you did not request this, please ignore this email.");

		mailSender.send(message);
	}

	@Override
	@Transactional(readOnly = true)
	public void verifyResetCode(VerifyCodeRequest request) {
		String cleanEmail = request.getEmail().trim().toLowerCase();

		Customer customer = customerRepository.findByEmailIgnoreCase(cleanEmail)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with provided email"));

		if (customer.getResetPasswordCode() == null
				|| !customer.getResetPasswordCode().equals(request.getCode().trim())) {
			throw new InvalidRequestException("Invalid verification code.");
		}

		if (customer.getResetCodeExpiry() == null || customer.getResetCodeExpiry().isBefore(LocalDateTime.now())) {
			throw new InvalidRequestException("Verification code has expired. Please request a new one.");
		}
	}

	@Override
	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		String cleanEmail = request.getEmail().trim().toLowerCase();

		Customer customer = customerRepository.findByEmailIgnoreCase(cleanEmail)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with provided email"));

		if (customer.getResetPasswordCode() == null
				|| !customer.getResetPasswordCode().equals(request.getCode().trim())) {
			throw new InvalidRequestException("Invalid verification code.");
		}

		if (customer.getResetCodeExpiry() == null || customer.getResetCodeExpiry().isBefore(LocalDateTime.now())) {
			throw new InvalidRequestException("Verification code has expired. Please request a new one.");
		}

		customer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		customer.setResetPasswordCode(null);
		customer.setResetCodeExpiry(null);
		customer.setModifiedAt(LocalDateTime.now());
		customerRepository.save(customer);
	}
}