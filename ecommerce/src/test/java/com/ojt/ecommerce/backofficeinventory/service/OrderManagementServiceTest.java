package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ojt.ecommerce.backofficeinventory.dto.OrderResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.OrderMapper;
import com.ojt.ecommerce.backofficeinventory.repository.OrderRepository;
import com.ojt.ecommerce.entity.Order;

@ExtendWith(MockitoExtension.class) // Mockito ကို သုံးမည်ဟု ကြေညာခြင်း
public class OrderManagementServiceTest {
	@Mock
	private OrderRepository orderRepository; // Database အစစ်အစား Mock (အတု) ကိုသုံးမည်
	@Mock
	private OrderMapper orderMapper;
	@InjectMocks
	private OrderManagementService orderManagementService; // Test လုပ်မည့် Service
	// ----------------------------------------------------
	// Method ၁: Status PENDING ဖြင့် ရှာသောအခါ စမ်းသပ်ခြင်း
	// ----------------------------------------------------

	@Test
	public void testGetOrders_WithPendingStatus() {
		// 1. Mock Data ပြင်ဆင်ခြင်း (Database ထဲသွားမထည့်ဘဲ ဒီမှာပဲ အတုလုပ်သည်)
		Order mockOrder = new Order();
		mockOrder.setOrderNo("ORD-001");
		mockOrder.setOrderStatus("PENDING");

		OrderResponseDto mockDto = OrderResponseDto.builder().orderNo("ORD-001").build();

		// 2. ဇာတ်တိုက်ခြင်း (Repository ကို ခေါ်ရင် ဒီ Data ပြန်ပေးရန်)
		when(orderRepository.findByOrderStatus("PENDING")).thenReturn(Arrays.asList(mockOrder));
		when(orderMapper.toDto(any(Order.class))).thenReturn(mockDto);

		// 3. တကယ်ခေါ်စမ်းကြည့်ခြင်း
		List<OrderResponseDto> result = orderManagementService.getOrders("PENDING");

		// 4. မှန်/မမှန် တိုက်စစ်ခြင်း (Assertion)
		assertEquals(1, result.size());
		assertEquals("ORD-001", result.get(0).getOrderNo());
	}

	// ----------------------------------------------------
	// Method ၂: Status အလွတ်ဖြင့် ရှာသောအခါ စမ်းသပ်ခြင်း (နောက်တစ်ခါ
	// ထပ်ရေးနိုင်သည်)
	// ----------------------------------------------------
	// ----------------------------------------------------
	// Method ၂: Status အလွတ် (Null) ဖြင့် ရှာသောအခါ စမ်းသပ်ခြင်း
	// ----------------------------------------------------
	@Test
	public void testGetOrders_WithNullStatus() {
		// 1. Mock Data ပြင်ဆင်ခြင်း (အော်ဒါ ၂ ခု အတုဖန်တီးမည်)
		Order mockOrder1 = new Order();
		mockOrder1.setOrderNo("ORD-001");
		mockOrder1.setOrderStatus("PENDING");

		Order mockOrder2 = new Order();
		mockOrder2.setOrderNo("ORD-002");
		mockOrder2.setOrderStatus("SHIPPED");

		OrderResponseDto mockDto1 = OrderResponseDto.builder().orderNo("ORD-001").build();
		OrderResponseDto mockDto2 = OrderResponseDto.builder().orderNo("ORD-002").build();

		// 2. ဇာတ်တိုက်ခြင်း (status က null ဖြစ်တဲ့အတွက် findAll() ကို ခေါ်လိမ့်မည်ဟု
		// ယူဆပြီး mock လုပ်သည်)
		when(orderRepository.findAll()).thenReturn(Arrays.asList(mockOrder1, mockOrder2));

		// Order ၂ ခုလုံးအတွက် Mapper ကို ဇာတ်တိုက်သည်
		when(orderMapper.toDto(mockOrder1)).thenReturn(mockDto1);
		when(orderMapper.toDto(mockOrder2)).thenReturn(mockDto2);

		// 3. တကယ်ခေါ်စမ်းကြည့်ခြင်း (null ထည့်ပေးလိုက်သည်)
		List<OrderResponseDto> result = orderManagementService.getOrders(null);

		// 4. မှန်/မမှန် တိုက်စစ်ခြင်း (Assertion)
		// Database ထဲမှာ အော်ဒါ ၂ ခုရှိတယ်လို့ ဇာတ်တိုက်ထားတဲ့အတွက် ပြန်ထွက်လာတဲ့
		// အရေအတွက်က ၂ ခု ဖြစ်ရပါမယ်
		assertEquals(2, result.size());
		assertEquals("ORD-001", result.get(0).getOrderNo());
		assertEquals("ORD-002", result.get(1).getOrderNo());
	}

	// ----------------------------------------------------
	// Method ၃: Status က စာသားအလွတ် ("") ဖြစ်နေသောအခါ စမ်းသပ်ခြင်း
	// ----------------------------------------------------
	@Test
	public void testGetOrders_WithEmptyStatus() {
		Order mockOrder1 = new Order();
		mockOrder1.setOrderNo("ORD-001");
		OrderResponseDto mockDto1 = OrderResponseDto.builder().orderNo("ORD-001").build();

		// Empty string ("") ပေးတဲ့အခါ if-condition ရဲ့ !status.trim().isEmpty() က false
		// ဖြစ်သွားပြီး else ကိုရောက်သွားမှာမို့ findAll() ကိုခေါ်ပါလိမ့်မယ်
		when(orderRepository.findAll()).thenReturn(Arrays.asList(mockOrder1));
		when(orderMapper.toDto(any(Order.class))).thenReturn(mockDto1);

		List<OrderResponseDto> result = orderManagementService.getOrders("");

		assertEquals(1, result.size());
	}

	// ----------------------------------------------------
	// Method ၄: Status က Space အလွတ် (" ") ဖြစ်နေသောအခါ စမ်းသပ်ခြင်း
	// ----------------------------------------------------
	@Test
	public void testGetOrders_WithBlankStatus() {
		Order mockOrder1 = new Order();
		mockOrder1.setOrderNo("ORD-001");
		OrderResponseDto mockDto1 = OrderResponseDto.builder().orderNo("ORD-001").build();

		// Space အလွတ် (" ") ကို trim() လုပ်လိုက်ရင် empty ဖြစ်သွားတဲ့အတွက် else
		// ကိုရောက်ပြီး findAll() ကိုပဲခေါ်ပါလိမ့်မယ်
		when(orderRepository.findAll()).thenReturn(Arrays.asList(mockOrder1));
		when(orderMapper.toDto(any(Order.class))).thenReturn(mockDto1);

		List<OrderResponseDto> result = orderManagementService.getOrders(" ");

		assertEquals(1, result.size());
	}
}