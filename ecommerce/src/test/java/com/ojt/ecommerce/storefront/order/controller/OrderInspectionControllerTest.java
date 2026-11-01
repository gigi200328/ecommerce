package com.ojt.ecommerce.storefront.order.controller;

import com.ojt.ecommerce.storefront.exception.AccessDeniedException;
import com.ojt.ecommerce.storefront.order.dto.ApiResponse;
import com.ojt.ecommerce.storefront.order.dto.CustomerOrderHistoryResponseDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderHistoryRequest;
import com.ojt.ecommerce.storefront.order.dto.OrderInvoiceResponseDTO;
import com.ojt.ecommerce.storefront.order.service.OrderInspectionService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderInspectionControllerTest {

    @Mock
    private OrderInspectionService orderInspectionService;

    @InjectMocks
    private OrderInspectionController orderInspectionController;

    private CustomerPrincipal principal;
    private CustomerOrderHistoryResponseDTO historyDTO;
    private OrderInvoiceResponseDTO invoiceDTO;

    @BeforeEach
    void setUp() {
        principal = new CustomerPrincipal(1L, "customer@example.com");

        historyDTO = CustomerOrderHistoryResponseDTO.builder()
                .totalItems(10)
                .totalPages(1)
                .currentPage(0)
                .pageSize(20)
                .first(true)
                .last(true)
                .orders(Collections.emptyList())
                .build();

        invoiceDTO = OrderInvoiceResponseDTO.builder()
                .orderId(100L)
                .orderNo("ORD-100")
                .items(Collections.emptyList())
                .build();
    }

    // =========================================================================
    // 1. getCustomerOrderHistory Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with order history when authenticated user requests their own orders")
    void getCustomerOrderHistory_Success() {
        OrderHistoryRequest request = new OrderHistoryRequest();
        request.setLimit(20);
        request.setOffset(0);

        when(orderInspectionService.getCustomerOrderHistory(1L, request)).thenReturn(historyDTO);

        ResponseEntity<ApiResponse<CustomerOrderHistoryResponseDTO>> response =
                orderInspectionController.getCustomerOrderHistory(1L, principal, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(10L, response.getBody().getData().getTotalItems());

        verify(orderInspectionService, times(1)).getCustomerOrderHistory(1L, request);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException (IDOR guard) when requested customerId does not match authenticated principal")
    void getCustomerOrderHistory_ThrowsAccessDenied_WhenIdorMismatch() {
        OrderHistoryRequest request = new OrderHistoryRequest();

        // Principal is customer 1L, but requesting customer 99L
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> orderInspectionController.getCustomerOrderHistory(99L, principal, request));

        assertTrue(ex.getMessage().contains("not authorised"));
        verifyNoInteractions(orderInspectionService);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when caller is unauthenticated")
    void getCustomerOrderHistory_ThrowsAccessDenied_WhenUnauthenticated() {
        OrderHistoryRequest request = new OrderHistoryRequest();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> orderInspectionController.getCustomerOrderHistory(null, null, request));

        assertTrue(ex.getMessage().contains("authentication required"));
        verifyNoInteractions(orderInspectionService);
    }

    // =========================================================================
    // 2. getOrderInvoiceDetail Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with invoice detail when authenticated user requests order invoice")
    void getOrderInvoiceDetail_Success() {
        when(orderInspectionService.getOrderInvoiceDetail(100L, 1L)).thenReturn(invoiceDTO);

        ResponseEntity<ApiResponse<OrderInvoiceResponseDTO>> response =
                orderInspectionController.getOrderInvoiceDetail(100L, 1L, principal);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ORD-100", response.getBody().getData().getOrderNo());

        verify(orderInspectionService, times(1)).getOrderInvoiceDetail(100L, 1L);
    }

    // =========================================================================
    // 3. downloadInvoicePdf Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with application/pdf header when exporting invoice PDF")
    void downloadInvoicePdf_Success() {
        byte[] pdfBytes = "%PDF-1.4 test invoice content".getBytes();
        when(orderInspectionService.exportInvoicePdf(100L, 1L)).thenReturn(pdfBytes);

        ResponseEntity<byte[]> response =
                orderInspectionController.downloadInvoicePdf(100L, 1L, principal);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertNotNull(response.getHeaders().getContentDisposition());
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("invoice-100.pdf"));
        assertArrayEquals(pdfBytes, response.getBody());

        verify(orderInspectionService, times(1)).exportInvoicePdf(100L, 1L);
    }

    // =========================================================================
    // 4. downloadOrderHistoryPdf Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with application/pdf header when exporting order history report PDF")
    void downloadOrderHistoryPdf_Success() {
        byte[] pdfBytes = "%PDF-1.4 test report content".getBytes();
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 20);

        when(orderInspectionService.exportOrderHistoryPdf(1L, start, end, "DELIVERED", "ORD-100"))
                .thenReturn(pdfBytes);

        ResponseEntity<byte[]> response = orderInspectionController.downloadOrderHistoryPdf(
                1L, start, end, "DELIVERED", "ORD-100", principal);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("order-history-report.pdf"));
        assertArrayEquals(pdfBytes, response.getBody());

        verify(orderInspectionService, times(1))
                .exportOrderHistoryPdf(1L, start, end, "DELIVERED", "ORD-100");
    }
}
