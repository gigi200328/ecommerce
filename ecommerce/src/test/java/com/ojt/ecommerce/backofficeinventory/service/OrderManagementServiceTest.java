package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ojt.ecommerce.backofficeinventory.dto.OrderResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.OrderStatusHistoryDto;
import com.ojt.ecommerce.backofficeinventory.dto.OrderStatusUpdateRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.PaginatedResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.PaymentVerificationDto;
import com.ojt.ecommerce.backofficeinventory.dto.ShipmentRequestDto;
import com.ojt.ecommerce.backofficeinventory.mapper.OrderMapper;
import com.ojt.ecommerce.backofficeinventory.mapper.PaymentMapper;
import com.ojt.ecommerce.backofficeinventory.mapper.ShipmentMapper;
import com.ojt.ecommerce.backofficeinventory.repository.OrderRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderStatusHistoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.PaymentRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ShipmentRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.entity.Payment;
import com.ojt.ecommerce.entity.Shipment;
import com.ojt.ecommerce.entity.User;
import com.ojt.ecommerce.enums.CourierName;

@ExtendWith(MockitoExtension.class)
class OrderManagementServiceTest {

	@Mock
	private OrderRepository orderRepository;
	@Mock
	private OrderMapper orderMapper;
	@Mock
	private OrderStatusHistoryRepository orderStatusHistoryRepository;
	@Mock
	private PaymentRepository paymentRepository;
	@Mock
	private ShipmentRepository shipmentRepository;
	@Mock
	private PaymentMapper paymentMapper;
	@Mock
	private ShipmentMapper shipmentMapper;
	@Mock
	private InventoryService inventoryService;
	@Mock
	private AdminNotificationService notificationService;

	@InjectMocks
	private OrderManagementService orderManagementService;

	private Order sampleOrder;
	private User mockUser;

	@BeforeEach
	void setUp() {
		// ⭐️ ၁။ Mock User အသစ်ဖန်တီးခြင်း
		mockUser = new User();
		mockUser.setUserName("admin");

		CustomUserDetails userDetails = new CustomUserDetails(mockUser);

		// ⭐️ ၂။ Authentication ကို Mock လုပ်ပြီး Principal ထည့်ပေးခြင်း
		Authentication authentication = mock(Authentication.class);
		lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
		// (တချို့ Spring Security version များတွင် getName() ကိုပါ ခေါ်တတ်သဖြင့်
		// ဖြည့်ထားခြင်းဖြစ်သည်)
		lenient().when(authentication.getName()).thenReturn("admin");

		// ⭐️ ၃။ SecurityContext အသစ်တစ်ခု တကယ် ဖန်တီးခြင်း (Mock မလုပ်တော့ပါ) ⭐️
		// (Mock လုပ်ပါက တစ်ခါတစ်လေ ပြဿနာတက်နိုင်သဖြင့် SecurityContextHolder မှ
		// အလွတ်တစ်ခုကို တောင်းယူပါသည်)
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(authentication);
		SecurityContextHolder.setContext(securityContext);

		// Prepare Sample Order Data
		Customer customer = new Customer();
		customer.setFullName("Test Customer");

		sampleOrder = new Order();
		sampleOrder.setOrderId(1L);
		sampleOrder.setOrderNo("ORD-12345");
		sampleOrder.setOrderStatus("PENDING");
		sampleOrder.setCustomer(customer);
		sampleOrder.setTotalAmount(new BigDecimal("10000"));
	}

	@AfterEach
	void tearDown() {
		// Test တစ်ခုပြီးတိုင်း Security Context ကို ရှင်းထုတ်ပါမည်
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("getOrders() - Should return paginated orders")
	void testGetOrders() {
		// Arrange
		Page<Order> mockPage = new PageImpl<>(List.of(sampleOrder));
		when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

		PaginatedResponseDto<OrderResponseDto> mockResponse = new PaginatedResponseDto<>();
		mockResponse.setContent(List.of(new OrderResponseDto()));
		when(orderMapper.toPaginatedDto(mockPage)).thenReturn(mockResponse);

		// Act
		PaginatedResponseDto<OrderResponseDto> result = orderManagementService.getOrders("PENDING", "ORD", null, null,
				0, 10);

		// Assert
		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		verify(orderRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
	}

	@Test
	@DisplayName("getOrderByOrderNo() - Should return order dto when found")
	void testGetOrderByOrderNo_Success() {
		// Arrange
		when(orderRepository.findByOrderNo("ORD-12345")).thenReturn(Optional.of(sampleOrder));
		OrderResponseDto dto = new OrderResponseDto();
		dto.setOrderNo("ORD-12345");
		when(orderMapper.toDto(sampleOrder)).thenReturn(dto);

		// Act
		OrderResponseDto result = orderManagementService.getOrderByOrderNo("ORD-12345");

		// Assert
		assertNotNull(result);
		assertEquals("ORD-12345", result.getOrderNo());
	}

	@Test
	@DisplayName("getOrderByOrderNo() - Should throw EntityNotFoundException when not found")
	void testGetOrderByOrderNo_NotFound() {
		// Arrange
		// ⭐️ နှစ်နေရာလုံးတွင် "UNKNOWN" သာ ဖြစ်ရပါမည် ⭐️
		when(orderRepository.findByOrderNo("UNKNOWN")).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
			orderManagementService.getOrderByOrderNo("UNKNOWN");
		});
	}

	@Test
	@DisplayName("updateOrderStatus() - Should update status to PAID and trigger notification")
	void testUpdateOrderStatus_ToPaid() {
		// Arrange
		when(orderRepository.findByOrderNo("ORD-12345")).thenReturn(Optional.of(sampleOrder));
		when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

		OrderStatusUpdateRequestDto request = new OrderStatusUpdateRequestDto();
		request.setNewStatus("PAID");
		request.setRemark("Payment received");

		// Act
		orderManagementService.updateOrderStatus("ORD-12345", request);

		// Assert
		assertEquals("PAID", sampleOrder.getOrderStatus());
		verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
		verify(inventoryService, times(1)).deductStockForOrder(1L); // Stock should be deducted
		verify(notificationService, times(1)).sendCustomNotification(eq("NEW_ORDER"), anyString(), anyString(),
				anyString(), eq(1L));
	}

	@Test
	@DisplayName("updateOrderStatus() - Should throw IllegalArgumentException for invalid transition")
	void testUpdateOrderStatus_InvalidTransition() {
		// Arrange
		when(orderRepository.findByOrderNo("ORD-12345")).thenReturn(Optional.of(sampleOrder));

		OrderStatusUpdateRequestDto request = new OrderStatusUpdateRequestDto();
		request.setNewStatus("DELIVERED"); // PENDING မှ DELIVERED သို့ မရပါ

		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> {
			orderManagementService.updateOrderStatus("ORD-12345", request);
		});
	}

	@Test
	@DisplayName("getPaymentInfoByOrderNo() - Should return payment info")
	void testGetPaymentInfoByOrderNo() {
		// Arrange
		Payment payment = new Payment();
		when(paymentRepository.findByOrder_OrderNo("ORD-12345")).thenReturn(Optional.of(payment));
		when(paymentMapper.toDto(payment)).thenReturn(new PaymentVerificationDto());

		// Act
		PaymentVerificationDto result = orderManagementService.getPaymentInfoByOrderNo("ORD-12345");

		// Assert
		assertNotNull(result);
		verify(paymentRepository, times(1)).findByOrder_OrderNo("ORD-12345");
	}

	@Test
	@DisplayName("createShipmentAndUpdateStatus() - Should create new shipment and update order to SHIPPED")
	void testCreateShipmentAndUpdateStatus() {
		// Arrange
		sampleOrder.setOrderStatus("PROCESSING");
		when(orderRepository.findByOrderNo("ORD-12345")).thenReturn(Optional.of(sampleOrder));

		ShipmentRequestDto requestDto = new ShipmentRequestDto();
		requestDto.setCourierName(CourierName.J_AND_T);
		requestDto.setTrackingNumber("TRK-123");

		Shipment newShipment = new Shipment();
		when(shipmentMapper.toEntity(requestDto, sampleOrder)).thenReturn(newShipment);

		// Act
		orderManagementService.createShipmentAndUpdateStatus("ORD-12345", requestDto);

		// Assert
		assertEquals("SHIPPED", sampleOrder.getOrderStatus());
		verify(shipmentRepository, times(1)).save(newShipment);
		verify(orderStatusHistoryRepository, times(1)).save(any(OrderStatusHistory.class));
		verify(inventoryService, times(1)).deductStockForOrder(1L);
		verify(notificationService, times(1)).sendCustomNotification(eq("SHIPPING_UPDATE"), anyString(), anyString(),
				anyString(), eq(1L));
	}

	@Test
	@DisplayName("getOrderStatusHistory() - Should return list of history DTOs")
	void testGetOrderStatusHistory() {
		// Arrange
		when(orderRepository.findByOrderNo("ORD-12345")).thenReturn(Optional.of(sampleOrder));

		OrderStatusHistory history = new OrderStatusHistory();
		when(orderStatusHistoryRepository.findByOrder_OrderIdOrderByChangedAtDesc(1L)).thenReturn(List.of(history));

		when(orderMapper.toHistoryDto(history)).thenReturn(new OrderStatusHistoryDto());

		// Act
		List<OrderStatusHistoryDto> result = orderManagementService.getOrderStatusHistory("ORD-12345");

		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());
	}

	@Test
	@DisplayName("getOrderStatusCounts() - Should aggregate counts properly")
	void testGetOrderStatusCounts() {
		// Arrange
		Object[] row1 = { "PENDING", 5L };
		Object[] row2 = { "PAID", 3L };
		List<Object[]> mockResult = Arrays.asList(row1, row2);

		when(orderRepository.countOrdersByStatus()).thenReturn(mockResult);

		// Act
		Map<String, Long> result = orderManagementService.getOrderStatusCounts();

		// Assert
		assertEquals(5L, result.get("PENDING"));
		assertEquals(3L, result.get("PAID"));
		assertEquals(8L, result.get("TOTAL")); // Total should be calculated automatically
	}

	@Test
	@DisplayName("updateOrderStatus() - Should throw IllegalArgumentException when status is the same")
	void testUpdateOrderStatus_SameStatus() {
		// Arrange
		when(orderRepository.findByOrderNo("ORD-12345")).thenReturn(Optional.of(sampleOrder)); // လက်ရှိမှာ PENDING
																								// ဖြစ်နေသည်

		OrderStatusUpdateRequestDto request = new OrderStatusUpdateRequestDto();
		request.setNewStatus("PENDING"); // ⬅️ PENDING ကိုပဲ ပြန်ပြောင်းဖို့ ကြိုးစားခြင်း

		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> {
			orderManagementService.updateOrderStatus("ORD-12345", request);
		});
	}
}