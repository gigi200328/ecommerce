package com.ojt.ecommerce.storefront.order.controller;

import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.order.dto.OrderDetailResponse;
import com.ojt.ecommerce.storefront.order.dto.OrderListResponse;
import com.ojt.ecommerce.storefront.order.service.OrderHistoryService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderHistoryControllerTest {

    @Mock
    private OrderHistoryService orderHistoryService;

    @InjectMocks
    private OrderHistoryController orderHistoryController;

    private CustomerPrincipal principal;
    private OrderListResponse sampleOrderListResponse;
    private OrderDetailResponse sampleOrderDetailResponse;

    @BeforeEach
    void setUp() {
        principal = new CustomerPrincipal(1L, "customer@example.com");

        sampleOrderListResponse = OrderListResponse.builder()
                .orderId(10L)
                .orderNo("ORD-1001")
                .orderStatus("DELIVERED")
                .paymentStatus("SUCCESS")
                .totalAmount(new BigDecimal("105000.00"))
                .createdAt("2026-10-20T10:00:00")
                .items(Collections.emptyList())
                .trackingNumbers(List.of("TRACK-123"))
                .build();

        sampleOrderDetailResponse = OrderDetailResponse.builder()
                .orderId(10L)
                .orderNo("ORD-1001")
                .orderStatus("DELIVERED")
                .paymentStatus("SUCCESS")
                .totalAmount(new BigDecimal("105000.00"))
                .items(Collections.emptyList())
                .shipments(Collections.emptyList())
                .statusHistory(Collections.emptyList())
                .build();
    }

    // =========================================================================
    // 1. getMyOrders Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with paginated orders when valid principal provided")
    void getMyOrders_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderListResponse> page = new PageImpl<>(List.of(sampleOrderListResponse), pageable, 1);

        when(orderHistoryService.getMyOrders(eq(1L), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        ResponseEntity<Page<OrderListResponse>> response = orderHistoryController.getMyOrders(
                principal, null, null, null, null, pageable);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals("ORD-1001", response.getBody().getContent().get(0).getOrderNo());

        verify(orderHistoryService, times(1))
                .getMyOrders(1L, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("Should pass filter parameters properly to OrderHistoryService")
    void getMyOrders_WithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderListResponse> page = new PageImpl<>(List.of(sampleOrderListResponse), pageable, 1);

        when(orderHistoryService.getMyOrders(eq(1L), eq("2026-10-01"), eq("2026-10-20"), eq("DELIVERED"), eq("ORD-1001"), eq(pageable)))
                .thenReturn(page);

        ResponseEntity<Page<OrderListResponse>> response = orderHistoryController.getMyOrders(
                principal, "2026-10-01", "2026-10-20", "DELIVERED", "ORD-1001", pageable);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalElements());

        verify(orderHistoryService, times(1))
                .getMyOrders(1L, "2026-10-01", "2026-10-20", "DELIVERED", "ORD-1001", pageable);
    }

    // =========================================================================
    // 2. getOrderById Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with order detail when order belongs to customer")
    void getOrderById_Success() {
        when(orderHistoryService.getOrderById(1L, 10L)).thenReturn(sampleOrderDetailResponse);

        ResponseEntity<OrderDetailResponse> response = orderHistoryController.getOrderById(principal, 10L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().getOrderId());
        assertEquals("ORD-1001", response.getBody().getOrderNo());

        verify(orderHistoryService, times(1)).getOrderById(1L, 10L);
    }

    @Test
    @DisplayName("Should propagate ResourceNotFoundException when order does not exist or unauthorized")
    void getOrderById_NotFound() {
        when(orderHistoryService.getOrderById(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderHistoryController.getOrderById(principal, 999L));

        assertEquals("Order not found", ex.getMessage());
        verify(orderHistoryService, times(1)).getOrderById(1L, 999L);
    }
}
