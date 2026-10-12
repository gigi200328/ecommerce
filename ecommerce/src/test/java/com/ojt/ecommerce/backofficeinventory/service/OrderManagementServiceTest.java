package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ojt.ecommerce.backofficeinventory.dto.OrderResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.OrderStatusUpdateRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.PaginatedResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.OrderMapper;
import com.ojt.ecommerce.backofficeinventory.repository.OrderRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderStatusHistoryRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.entity.User;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class OrderManagementServiceTest {
	@Mock
	private OrderRepository orderRepository;
	@Mock
	private OrderStatusHistoryRepository orderStatusHistoryRepository;
	@Mock
	private OrderMapper orderMapper;
	@InjectMocks
	private OrderManagementService orderManagementService;

	@Test
	public void testGetOrders_WithPendingStatus() {
		Order mockOrder = new Order();
		mockOrder.setOrderNo("ORD-001");
		mockOrder.setOrderStatus("PENDING");

		OrderResponseDto mockDto = OrderResponseDto.builder().orderNo("ORD-001").build();
		PaginatedResponseDto<OrderResponseDto> mockPaginated = PaginatedResponseDto.<OrderResponseDto>builder()
				.content(Arrays.asList(mockDto)).totalElements(1L).build();

		when(orderRepository.findByOrderStatus(eq("PENDING"), any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(mockOrder)));
		when(orderMapper.toPaginatedDto(any())).thenReturn(mockPaginated);

		PaginatedResponseDto<OrderResponseDto> result = orderManagementService.getOrders("PENDING", 0, 10);

		assertEquals(1, result.getContent().size());
		assertEquals("ORD-001", result.getContent().get(0).getOrderNo());
	}

	@Test
	public void testGetOrders_WithNullStatus() {
		Order mockOrder1 = new Order();
		mockOrder1.setOrderNo("ORD-001");
		mockOrder1.setOrderStatus("PENDING");

		Order mockOrder2 = new Order();
		mockOrder2.setOrderNo("ORD-002");
		mockOrder2.setOrderStatus("SHIPPED");

		OrderResponseDto mockDto1 = OrderResponseDto.builder().orderNo("ORD-001").build();
		OrderResponseDto mockDto2 = OrderResponseDto.builder().orderNo("ORD-002").build();
		PaginatedResponseDto<OrderResponseDto> mockPaginated = PaginatedResponseDto.<OrderResponseDto>builder()
				.content(Arrays.asList(mockDto1, mockDto2)).totalElements(2L).build();

		when(orderRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(mockOrder1, mockOrder2)));
		when(orderMapper.toPaginatedDto(any())).thenReturn(mockPaginated);

		PaginatedResponseDto<OrderResponseDto> result = orderManagementService.getOrders(null, 0, 10);

		assertEquals(2, result.getContent().size());
		assertEquals("ORD-001", result.getContent().get(0).getOrderNo());
		assertEquals("ORD-002", result.getContent().get(1).getOrderNo());
	}

	@Test
	public void testGetOrders_WithEmptyStatus() {
		Order mockOrder1 = new Order();
		mockOrder1.setOrderNo("ORD-001");
		OrderResponseDto mockDto1 = OrderResponseDto.builder().orderNo("ORD-001").build();
		PaginatedResponseDto<OrderResponseDto> mockPaginated = PaginatedResponseDto.<OrderResponseDto>builder()
				.content(Arrays.asList(mockDto1)).totalElements(1L).build();

		when(orderRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(mockOrder1)));
		when(orderMapper.toPaginatedDto(any())).thenReturn(mockPaginated);

		PaginatedResponseDto<OrderResponseDto> result = orderManagementService.getOrders("", 0, 10);

		assertEquals(1, result.getContent().size());
	}

	@Test
	public void testGetOrders_WithBlankStatus() {
		Order mockOrder1 = new Order();
		mockOrder1.setOrderNo("ORD-001");
		OrderResponseDto mockDto1 = OrderResponseDto.builder().orderNo("ORD-001").build();
		PaginatedResponseDto<OrderResponseDto> mockPaginated = PaginatedResponseDto.<OrderResponseDto>builder()
				.content(Arrays.asList(mockDto1)).totalElements(1L).build();

		when(orderRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(mockOrder1)));
		when(orderMapper.toPaginatedDto(any())).thenReturn(mockPaginated);

		PaginatedResponseDto<OrderResponseDto> result = orderManagementService.getOrders(" ", 0, 10);

		assertEquals(1, result.getContent().size());
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
