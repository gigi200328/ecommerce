package com.ojt.ecommerce.storefront.order.service;

import com.ojt.ecommerce.storefront.exception.AccessDeniedException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.order.dto.CustomerOrderHistoryResponseDTO;
import com.ojt.ecommerce.storefront.order.dto.CustomerOrderHistoryResponseDTO.OrderSummaryDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderHistoryRequest;
import com.ojt.ecommerce.storefront.order.dto.OrderInvoiceResponseDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderInvoiceResponseDTO.OrderItemDetailDTO;
import com.ojt.ecommerce.storefront.order.repository.OrderInspectionRepository;
import com.ojt.ecommerce.storefront.order.repository.OrderInspectionRepository.InvoiceHeaderRow;
import com.ojt.ecommerce.storefront.order.repository.OrderInspectionRepository.InvoiceRawResult;
import com.ojt.ecommerce.storefront.order.repository.OrderInspectionRepository.OrderHistoryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderInspectionServiceImplTest {

    @Mock
    private OrderInspectionRepository repository;

    @InjectMocks
    private OrderInspectionServiceImpl orderInspectionService;

    private OrderSummaryDTO orderSummary1;
    private OrderSummaryDTO orderSummary2;
    private InvoiceHeaderRow sampleHeader;
    private OrderItemDetailDTO sampleItem;

    @BeforeEach
    void setUp() {
        orderSummary1 = OrderSummaryDTO.builder()
                .orderId(101L)
                .orderNo("ORD-2026-0001")
                .orderStatus("DELIVERED")
                .paymentStatus("SUCCESS")
                .createdAt(LocalDateTime.now().minusDays(3))
                .subtotalAmount(new BigDecimal("100000.00"))
                .shippingFee(new BigDecimal("3000.00"))
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("103000.00"))
                .totalLineItems(2)
                .totalItemQuantity(3)
                .latestPaymentMethod("KBZ_PAY")
                .build();

        orderSummary2 = OrderSummaryDTO.builder()
                .orderId(102L)
                .orderNo("ORD-2026-0002")
                .orderStatus("PROCESSING")
                .paymentStatus("SUCCESS")
                .createdAt(LocalDateTime.now().minusDays(1))
                .subtotalAmount(new BigDecimal("50000.00"))
                .shippingFee(new BigDecimal("2500.00"))
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("52500.00"))
                .totalLineItems(1)
                .totalItemQuantity(1)
                .latestPaymentMethod("CASH_ON_DELIVERY")
                .build();

        sampleHeader = new InvoiceHeaderRow(
                101L,
                "ORD-2026-0001",
                "DELIVERED",
                "SUCCESS",
                LocalDateTime.of(2026, 10, 10, 14, 30),
                LocalDateTime.of(2026, 10, 12, 10, 0),
                new BigDecimal("100000.00"),
                new BigDecimal("3000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("103000.00"),
                1L,
                "John Doe",
                "john@example.com",
                "09123456789",
                "John Doe",
                "09123456789",
                "No. 123, Bogyoke Road",
                "Apt 4B",
                "Kyauktada",
                "Yangon",
                "Yangon Region",
                "No. 123, Bogyoke Road, Apt 4B, Kyauktada, Yangon, Yangon Region",
                201L,
                "KBZ_PAY",
                "SUCCESS",
                "TXN-987654",
                LocalDateTime.of(2026, 10, 10, 14, 35),
                new BigDecimal("103000.00"),
                301L,
                "Ninja Van",
                "NINJA-TRACK-001",
                "DELIVERED",
                LocalDateTime.of(2026, 10, 11, 9, 0),
                LocalDateTime.of(2026, 10, 12, 10, 0)
        );

        sampleItem = OrderItemDetailDTO.builder()
                .orderItemId(501L)
                .productName("Premium Wireless Earbuds")
                .sku("SKU-EARBUDS-BLK")
                .variantAttributes("{\"Color\":\"Black\"}")
                .unitPrice(new BigDecimal("50000.00"))
                .qty(2)
                .discountAmount(BigDecimal.ZERO)
                .subtotal(new BigDecimal("100000.00"))
                .build();
    }

    // =========================================================================
    // 1. getCustomerOrderHistory Tests (Pagination & SP Result Mapping)
    // =========================================================================

    @Test
    @DisplayName("Should return order history with correctly computed pagination metadata on first page")
    void getCustomerOrderHistory_Success_FirstPage() {
        OrderHistoryRequest request = new OrderHistoryRequest();
        request.setLimit(2);
        request.setOffset(0);
        request.setOrderStatus("DELIVERED");

        OrderHistoryResult result = new OrderHistoryResult(List.of(orderSummary1, orderSummary2), 5L);
        when(repository.callGetCustomerOrderHistory(1L, 2, 0, "DELIVERED")).thenReturn(result);

        CustomerOrderHistoryResponseDTO response = orderInspectionService.getCustomerOrderHistory(1L, request);

        assertNotNull(response);
        assertEquals(5L, response.getTotalItems());
        assertEquals(3, response.getTotalPages()); // ceil(5/2) = 3
        assertEquals(0, response.getCurrentPage()); // offset 0 / limit 2 = 0
        assertEquals(2, response.getPageSize());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());
        assertEquals(2, response.getOrders().size());
        assertEquals("ORD-2026-0001", response.getOrders().get(0).getOrderNo());
    }

    @Test
    @DisplayName("Should return correct pagination flags for middle page")
    void getCustomerOrderHistory_MiddlePage() {
        OrderHistoryRequest request = new OrderHistoryRequest();
        request.setLimit(2);
        request.setOffset(2);

        OrderHistoryResult result = new OrderHistoryResult(List.of(orderSummary1), 5L);
        when(repository.callGetCustomerOrderHistory(1L, 2, 2, null)).thenReturn(result);

        CustomerOrderHistoryResponseDTO response = orderInspectionService.getCustomerOrderHistory(1L, request);

        assertNotNull(response);
        assertEquals(1, response.getCurrentPage()); // offset 2 / limit 2 = 1
        assertFalse(response.isFirst());
        assertFalse(response.isLast());
    }

    @Test
    @DisplayName("Should return isLast true when offset + limit >= totalItems")
    void getCustomerOrderHistory_LastPage() {
        OrderHistoryRequest request = new OrderHistoryRequest();
        request.setLimit(2);
        request.setOffset(4);

        OrderHistoryResult result = new OrderHistoryResult(List.of(orderSummary1), 5L);
        when(repository.callGetCustomerOrderHistory(1L, 2, 4, null)).thenReturn(result);

        CustomerOrderHistoryResponseDTO response = orderInspectionService.getCustomerOrderHistory(1L, request);

        assertNotNull(response);
        assertEquals(2, response.getCurrentPage());
        assertFalse(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    @DisplayName("Should return empty pagination DTO when SP returns no matching orders")
    void getCustomerOrderHistory_Empty_ReturnsEmptyResponse() {
        OrderHistoryRequest request = new OrderHistoryRequest();
        request.setLimit(20);
        request.setOffset(0);

        OrderHistoryResult emptyResult = new OrderHistoryResult(Collections.emptyList(), 0L);
        when(repository.callGetCustomerOrderHistory(1L, 20, 0, null)).thenReturn(emptyResult);

        CustomerOrderHistoryResponseDTO response = orderInspectionService.getCustomerOrderHistory(1L, request);

        assertNotNull(response);
        assertEquals(0L, response.getTotalItems());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getCurrentPage());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
        assertTrue(response.getOrders().isEmpty());
    }

    @Test
    @DisplayName("Should sanitize null limit, offset, and trim orderStatus filter")
    void getCustomerOrderHistory_SanitizesInputs() {
        OrderHistoryRequest request = new OrderHistoryRequest();
        request.setLimit(null); // should default to 20
        request.setOffset(null); // should default to 0
        request.setOrderStatus("  paid  "); // should trim and uppercase to "PAID"

        OrderHistoryResult result = new OrderHistoryResult(List.of(orderSummary1), 1L);
        when(repository.callGetCustomerOrderHistory(1L, 20, 0, "PAID")).thenReturn(result);

        CustomerOrderHistoryResponseDTO response = orderInspectionService.getCustomerOrderHistory(1L, request);

        assertNotNull(response);
        verify(repository, times(1)).callGetCustomerOrderHistory(1L, 20, 0, "PAID");
    }

    @Test
    @DisplayName("Should cap limit at 100 when excessive limit is requested")
    void getCustomerOrderHistory_CapsLimitAt100() {
        OrderHistoryRequest request = new OrderHistoryRequest();
        request.setLimit(500); // capped at 100
        request.setOffset(-10); // reset to 0

        OrderHistoryResult result = new OrderHistoryResult(List.of(orderSummary1), 1L);
        when(repository.callGetCustomerOrderHistory(1L, 100, 0, null)).thenReturn(result);

        orderInspectionService.getCustomerOrderHistory(1L, request);

        verify(repository, times(1)).callGetCustomerOrderHistory(1L, 100, 0, null);
    }

    @Test
    @DisplayName("Should translate DataAccessException into ResourceNotFoundException")
    void getCustomerOrderHistory_TranslatesDataAccessException() {
        OrderHistoryRequest request = new OrderHistoryRequest();
        when(repository.callGetCustomerOrderHistory(anyLong(), anyInt(), anyInt(), any()))
                .thenThrow(new DataRetrievalFailureException("Customer with ID 999 does not exist"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderInspectionService.getCustomerOrderHistory(999L, request));

        assertTrue(ex.getMessage().contains("Customer with ID 999 does not exist"));
    }

    // =========================================================================
    // 2. getOrderInvoiceDetail Tests (Two Result Sets & IDOR)
    // =========================================================================

    @Test
    @DisplayName("Should return full order invoice detail including payment and shipment tracking")
    void getOrderInvoiceDetail_Success_FullHeaderAndItems() {
        InvoiceRawResult rawResult = new InvoiceRawResult(
                Optional.of(sampleHeader),
                List.of(sampleItem)
        );
        when(repository.callGetOrderInvoiceDetail(101L, 1L)).thenReturn(rawResult);

        OrderInvoiceResponseDTO dto = orderInspectionService.getOrderInvoiceDetail(101L, 1L);

        assertNotNull(dto);
        assertEquals(101L, dto.getOrderId());
        assertEquals("ORD-2026-0001", dto.getOrderNo());
        assertEquals("DELIVERED", dto.getOrderStatus());
        assertEquals("SUCCESS", dto.getPaymentStatus());
        assertEquals(new BigDecimal("103000.00"), dto.getTotalAmount());
        assertEquals("John Doe", dto.getCustomerName());

        // Address mapping check
        assertNotNull(dto.getShippingAddress());
        assertEquals("Yangon", dto.getShippingAddress().getCity());
        assertEquals("Kyauktada", dto.getShippingAddress().getTownship());
        assertEquals("No. 123, Bogyoke Road", dto.getShippingAddress().getAddressLine1());

        // Payment mapping check
        assertNotNull(dto.getPaymentInfo());
        assertEquals("KBZ_PAY", dto.getPaymentInfo().getPaymentMethod());
        assertEquals("TXN-987654", dto.getPaymentInfo().getTransactionRef());

        // Shipment tracking mapping check
        assertNotNull(dto.getShipmentTracking());
        assertEquals("Ninja Van", dto.getShipmentTracking().getCourierName());
        assertEquals("NINJA-TRACK-001", dto.getShipmentTracking().getTrackingNumber());

        // Line items check
        assertEquals(1, dto.getItems().size());
        assertEquals("Premium Wireless Earbuds", dto.getItems().get(0).getProductName());
        assertEquals(2, dto.getItems().get(0).getQty());
    }

    @Test
    @DisplayName("Should handle orders with null payment and shipment gracefully")
    void getOrderInvoiceDetail_Success_WhenPaymentAndShipmentNull() {
        InvoiceHeaderRow pendingHeader = new InvoiceHeaderRow(
                102L, "ORD-2026-0002", "PENDING", "PENDING",
                LocalDateTime.now(), null,
                new BigDecimal("50000.00"), new BigDecimal("2500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("52500.00"),
                1L, "Jane Doe", "jane@example.com", "09987654321",
                "Jane Doe", "09987654321", "Mandalay Street", null,
                "Chanayethazan", "Mandalay", "Mandalay Region", "Mandalay Street, Mandalay",
                null, null, null, null, null, null, // Payment is null
                null, null, null, null, null, null  // Shipment is null
        );

        InvoiceRawResult rawResult = new InvoiceRawResult(
                Optional.of(pendingHeader),
                Collections.emptyList()
        );
        when(repository.callGetOrderInvoiceDetail(102L, 1L)).thenReturn(rawResult);

        OrderInvoiceResponseDTO dto = orderInspectionService.getOrderInvoiceDetail(102L, 1L);

        assertNotNull(dto);
        assertNull(dto.getPaymentInfo());
        assertNull(dto.getShipmentTracking());
        assertTrue(dto.getItems().isEmpty());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when header is missing")
    void getOrderInvoiceDetail_ThrowsNotFound_WhenHeaderMissing() {
        InvoiceRawResult emptyResult = new InvoiceRawResult(Optional.empty(), Collections.emptyList());
        when(repository.callGetOrderInvoiceDetail(999L, 1L)).thenReturn(emptyResult);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderInspectionService.getOrderInvoiceDetail(999L, 1L));

        assertEquals("Order not found or does not belong to this customer.", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when SP raises Access Denied signal")
    void getOrderInvoiceDetail_ThrowsAccessDenied_OnSignalMismatch() {
        when(repository.callGetOrderInvoiceDetail(101L, 99L))
                .thenThrow(new DataRetrievalFailureException("Access Denied: Customer does not own order"));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> orderInspectionService.getOrderInvoiceDetail(101L, 99L));

        assertTrue(ex.getMessage().contains("Access Denied"));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when SP raises other error")
    void getOrderInvoiceDetail_ThrowsNotFound_OnOtherSignalError() {
        when(repository.callGetOrderInvoiceDetail(999L, 1L))
                .thenThrow(new DataRetrievalFailureException("Order with ID 999 does not exist"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderInspectionService.getOrderInvoiceDetail(999L, 1L));

        assertTrue(ex.getMessage().contains("Order with ID 999 does not exist"));
    }
}
