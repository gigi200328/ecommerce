package com.ojt.ecommerce.storefront.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojt.ecommerce.storefront.auth.dto.*;
import com.ojt.ecommerce.storefront.auth.service.CustomerAuthService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerAuthControllerTest {

	@Mock
	private CustomerAuthService authService;

	@InjectMocks
	private CustomerAuthController customerAuthController;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	private static final String BASE = "/api/storefront/v1/auth/customer";

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
		objectMapper = new ObjectMapper();
		mockMvc = MockMvcBuilders.standaloneSetup(customerAuthController)
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper)).build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private UsernamePasswordAuthenticationToken createAuth() {
		CustomerPrincipal principal = new CustomerPrincipal(1L, "mgmg@example.com");
		return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
	}

	// ရည်ရွယ်ချက် - မှတ်ပုံတင်ခြင်း (Register) လုပ်ငန်းစဉ် အောင်မြင်ပါက HTTP Status
	// 200 နှင့် ကိုယ်ရေးအချက်အလက် (Token, Email) များ မှန်ကန်စွာ ပြန်လာခြင်း
	// ရှိ/မရှိ စစ်ဆေးခြင်း
	@Test
	void register_returns200AndAuthResponse() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFullName("Mg Mg");
		request.setEmail("mgmg@example.com");
		request.setPhone("09123456789");
		request.setPassword("password123");

		AuthResponse response = AuthResponse.builder().token("jwt-token-xyz").customerId(1L).fullName("Mg Mg")
				.email("mgmg@example.com").build();

		when(authService.register(any(RegisterRequest.class))).thenReturn(response);

		mockMvc.perform(post(BASE + "/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)).accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk()).andExpect(jsonPath("$.token").value("jwt-token-xyz"))
				.andExpect(jsonPath("$.email").value("mgmg@example.com"));

		verify(authService, times(1)).register(any(RegisterRequest.class));
	}

	// ရည်ရွယ်ချက် - အကောင့်ဝင်ရောက်ခြင်း (Login) အောင်မြင်ပါက HTTP Status 200 နှင့်
	// JWT Token မှန်ကန်စွာ ပြန်လာခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
	@Test
	void login_returns200AndAuthResponse() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail("mgmg@example.com");
		request.setPassword("password123");

		AuthResponse response = AuthResponse.builder().token("jwt-token-xyz").customerId(1L).email("mgmg@example.com")
				.build();

		when(authService.login(any(LoginRequest.class))).thenReturn(response);

		mockMvc.perform(post(BASE + "/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)).accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk()).andExpect(jsonPath("$.token").value("jwt-token-xyz"));

		verify(authService, times(1)).login(any(LoginRequest.class));
	}

	// ရည်ရွယ်ချက် - လက်ရှိ Login ဝင်ထားသော ဖောက်သည်အချက်အလက်များကို ဆွဲထုတ်ခြင်း
	// (Profile ကြည့်ခြင်း) အောင်မြင်ပါက HTTP Status 200 နှင့် Customer ID, Email
	// များ မှန်ကန်စွာ ပြန်လာခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
	@Test
	void getCurrentCustomer_returns200() throws Exception {
		CustomerMeResponse meResponse = CustomerMeResponse.builder().customerId(1L).fullName("Mg Mg")
				.email("mgmg@example.com").build();

		// Authenticated customer ID ကို အတိအကျ စစ်ဆေးပါ
		when(authService.getCurrentCustomer(1L)).thenReturn(meResponse);

		UsernamePasswordAuthenticationToken authentication = createAuth();
		SecurityContextHolder.getContext().setAuthentication(authentication);

		mockMvc.perform(get(BASE + "/me").accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("$.customerId").value(1)).andExpect(jsonPath("$.email").value("mgmg@example.com"));

		// Service ကို customer ID 1L ဖြင့် ခေါ်ထားကြောင်း စစ်ဆေးပါ
		verify(authService, times(1)).getCurrentCustomer(1L);
	}

	// ရည်ရွယ်ချက် - ဖောက်သည်၏ လျှို့ဝှက်နံပါတ် (Password) ပြောင်းလဲခြင်း
	// လုပ်ငန်းစဉ် အောင်မြင်ပါက HTTP Status 200 ပြန်လာခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
	@Test
	void changePassword_returns200() throws Exception {
		ChangePasswordRequest request = new ChangePasswordRequest();
		request.setCurrentPassword("oldPass");
		request.setNewPassword("newPass123");

		// void method အတွက် customer ID ကို အတိအကျ စစ်ဆေးပါ
		doNothing().when(authService).changePassword(eq(1L), any(ChangePasswordRequest.class));

		UsernamePasswordAuthenticationToken authentication = createAuth();
		SecurityContextHolder.getContext().setAuthentication(authentication);

		mockMvc.perform(put(BASE + "/change-password").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andDo(print()).andExpect(status().isOk());

		verify(authService, times(1)).changePassword(eq(1L), any(ChangePasswordRequest.class));
	}
}