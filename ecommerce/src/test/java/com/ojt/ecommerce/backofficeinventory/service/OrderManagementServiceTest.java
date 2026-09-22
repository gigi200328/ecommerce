package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ojt.ecommerce.backofficeinventory.dto.OrderResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.OrderStatusUpdateRequestDto;
import com.ojt.ecommerce.backofficeinventory.mapper.OrderMapper;
import com.ojt.ecommerce.backofficeinventory.repository.OrderRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderStatusHistoryRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.entity.User;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class) // Mockito ကို သုံးမည်ဟု ကြေညာခြင်း
public class OrderManagementServiceTest {
	@Mock
	private OrderRepository orderRepository; // Database အစစ်အစား Mock (အတု) ကိုသုံးမည်
	@Mock
	private OrderStatusHistoryRepository orderStatusHistoryRepository;
	@Mock
	private OrderMapper orderMapper;
	@InjectMocks
	private OrderManagementService orderManagementService; // Test လုပ်မည့် Service

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

	@Test
	public void testGetOrderByOrderNo_Success() {
		String orderNo = "ORD-001";
		Order mockOrder = new Order();
		mockOrder.setOrderNo(orderNo);

		OrderResponseDto mockDto = new OrderResponseDto();
		mockDto.setOrderNo(orderNo);

		when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.of(mockOrder));
		when(orderMapper.toDto(mockOrder)).thenReturn(mockDto);

		OrderResponseDto result = orderManagementService.getOrderByOrderNo(orderNo);

		assertNotNull(result);
		assertEquals(orderNo, result.getOrderNo());
		verify(orderRepository, times(1)).findByOrderNo(orderNo);
	}

	@Test
	void testGetOrderByOrderNo_NotFound() {
		String orderNo = "INVALID-ORD";
		when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.empty());

		assertThrows(EntityNotFoundException.class, () -> {
			orderManagementService.getOrderByOrderNo(orderNo);
		});

		verify(orderRepository, times(1)).findByOrderNo(orderNo);
	}

	@Test
	void testUpdateOrderStatus_Success() {
		// Arrange (1) - Order Data
		String orderNo = "ORD-001";
		Order mockOrder = new Order();
		mockOrder.setOrderNo(orderNo);
		mockOrder.setOrderStatus("PENDING");

		OrderStatusUpdateRequestDto requestDto = new OrderStatusUpdateRequestDto();
		requestDto.setNewStatus("SHIPPED");
		requestDto.setRemark("Dispatched");

		// Arrange (2) - Mock SecurityContextHolder (Admin နာမည်ယူရန်အတွက်)
		Authentication authentication = mock(Authentication.class);
		SecurityContext securityContext = mock(SecurityContext.class);

		User mockAdminUser = new User();
		mockAdminUser.setUserName("System Admin");

		CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
		when(mockUserDetails.getUser()).thenReturn(mockAdminUser);

		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(mockUserDetails);
		SecurityContextHolder.setContext(securityContext);

		when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.of(mockOrder));
		when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

		orderManagementService.updateOrderStatus(orderNo, requestDto);

		assertEquals("SHIPPED", mockOrder.getOrderStatus()); // Status ပြောင်းသွားကြောင်း စစ်ဆေးခြင်း
		verify(orderRepository, times(1)).save(mockOrder); // Save method ခေါ်သွားကြောင်း စစ်ဆေးခြင်း
		verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));

		SecurityContextHolder.clearContext();
	}
}