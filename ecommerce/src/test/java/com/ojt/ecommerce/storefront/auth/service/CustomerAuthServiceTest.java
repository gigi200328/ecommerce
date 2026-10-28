package com.ojt.ecommerce.storefront.auth.service;

import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.storefront.auth.dto.*;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import com.ojt.ecommerce.storefront.exception.ConflictException;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.security.G5JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTest {

	@Mock
	private CustomerRepository customerRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private G5JwtUtil jwtUtil;
	@Mock
	private JavaMailSender mailSender;

	@InjectMocks
	private CustomerAuthServiceImpl customerAuthService;

	@BeforeEach
	void setUp() {
		// Test လုပ်နေစဉ်အတွင်း @Value ဖြင့် ထည့်သွင်းထားသော senderEmail ကို
		// ရယူအသုံးပြုနိုင်ရန် သတ်မှတ်ပေးခြင်း
		ReflectionTestUtils.setField(customerAuthService, "senderEmail", "noreply@test.com");
	}

	// ရည်ရွယ်ချက် - Register လုပ်ငန်းစဉ် အောင်မြင်ပါက ဖန်တီးထားသော JWT Token နှင့်
	// Customer အချက်အလက်များ မှန်ကန်စွာ ပြန်လာခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
	@Test
	void register_success() {
		RegisterRequest request = new RegisterRequest();
		request.setFullName("Aung Aung");
		request.setEmail("aungaung@example.com");
		request.setPhone("09987654321");
		request.setPassword("password123");

		when(customerRepository.existsByEmail("aungaung@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

		Customer savedCustomer = Customer.builder().customerId(1L).fullName("Aung Aung").email("aungaung@example.com")
				.phone("09987654321").status("ACTIVE").build();

		when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
		when(jwtUtil.generateToken("aungaung@example.com", 1L)).thenReturn("mock-jwt-token");

		AuthResponse response = customerAuthService.register(request);

		assertNotNull(response);
		assertEquals("mock-jwt-token", response.getToken());
		assertEquals(1L, response.getCustomerId());
		assertEquals("aungaung@example.com", response.getEmail());

		verify(customerRepository).existsByEmail("aungaung@example.com");
		verify(customerRepository).save(any(Customer.class));
	}

	// ရည်ရွယ်ချက် - မှတ်ပုံတင်ရာတွင် Email သည် အသုံးပြုပြီးသား (Already in use)
	// ဖြစ်နေပါက ConflictException Error ပြန်ထုတ်ပေးခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
	@Test
	void register_emailAlreadyInUse_throwsConflictException() {
		RegisterRequest request = new RegisterRequest();
		request.setEmail("exists@example.com");
		request.setPassword("password");

		when(customerRepository.existsByEmail("exists@example.com")).thenReturn(true);

		assertThrows(ConflictException.class, () -> customerAuthService.register(request));
		verify(customerRepository, never()).save(any(Customer.class));
	}

	// ရည်ရွယ်ချက် - Login ဝင်ရောက်ခြင်း အောင်မြင်ပါက သင့်လျော်သော JWT Token
	// ထုတ်ပေးခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
	@Test
	void login_success() {
		LoginRequest request = new LoginRequest();
		request.setEmail("test@example.com");
		request.setPassword("password123");

		Customer customer = Customer.builder().customerId(1L).email("test@example.com").passwordHash("encodedPassword")
				.status("ACTIVE").build();

		when(customerRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(customer));
		when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
		when(jwtUtil.generateToken("test@example.com", 1L)).thenReturn("jwt-token");

		AuthResponse response = customerAuthService.login(request);

		assertNotNull(response);
		assertEquals("jwt-token", response.getToken());
		verify(customerRepository).findByEmailIgnoreCase("test@example.com");
	}

	// ရည်ရွယ်ချက် - Login ဝင်ရောက်ရာတွင် လျှို့ဝှက်နံပါတ် (Password) မှားယွင်းနေပါက
	// InvalidRequestException Error ပြန်ထုတ်ပေးခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
	@Test
	void login_invalidPassword_throwsInvalidRequestException() {
		LoginRequest request = new LoginRequest();
		request.setEmail("test@example.com");
		request.setPassword("wrongpassword");

		Customer customer = Customer.builder().email("test@example.com").passwordHash("encodedPassword")
				.status("ACTIVE").build();

		when(customerRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(customer));
		when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

		assertThrows(InvalidRequestException.class, () -> customerAuthService.login(request));
	}

	// ရည်ရွယ်ချက် - လက်ရှိ Login ဝင်ထားသော ဖောက်သည်အချက်အလက်များကို ဆွဲထုတ်ရာတွင်
	// မှန်ကန်သော အချက်အလက်များ ပြန်လာခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
	@Test
	void getCurrentCustomer_success() {
		Customer customer = Customer.builder().customerId(1L).fullName("Su Su").email("susu@example.com")
				.phone("09111222333").build();

		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

		CustomerMeResponse response = customerAuthService.getCurrentCustomer(1L);

		assertNotNull(response);
		assertEquals(1L, response.getCustomerId());
		assertEquals("Su Su", response.getFullName());
	}

	// ရည်ရွယ်ချက် - ဖောက်သည်အချက်အလက် ရှာဖွေရာတွင် မတွေ့ရှိပါက
	// ResourceNotFoundException Error ပြန်ထုတ်ပေးခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
	@Test
	void getCurrentCustomer_notFound_throwsResourceNotFoundException() {
		when(customerRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> customerAuthService.getCurrentCustomer(99L));
	}
}