package com.ojt.ecommerce.storefront.order.service;

import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.order.dto.OrderHistoryReportDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderInvoiceResponseDTO.OrderItemDetailDTO;
import com.ojt.ecommerce.storefront.order.repository.OrderInspectionRepository;
import com.ojt.ecommerce.storefront.order.repository.OrderInspectionRepository.InvoiceHeaderRow;
import com.ojt.ecommerce.storefront.order.repository.OrderInspectionRepository.InvoiceRawResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoicePdfGenerationTest {

    @Mock
    private OrderInspectionRepository repository;

    @InjectMocks
    private OrderInspectionServiceImpl orderInspectionService;

    private InvoiceHeaderRow completeHeader;
    private OrderItemDetailDTO item1;
    private OrderItemDetailDTO item2;
    private OrderHistoryReportDTO reportRow1;
    private OrderHistoryReportDTO reportRow2;

    @BeforeEach
    void setUp() {
        completeHeader = new InvoiceHeaderRow(
                101L,
                "ORD-2026-0001",
                "DELIVERED",
                "SUCCESS",
                LocalDateTime.of(2026, 10, 10, 14, 30),
                LocalDateTime.of(2026, 10, 12, 10, 0),
                new BigDecimal("150000.00"),
                new BigDecimal("3000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("153000.00"),
                1L,
                "Aung Aung",
                "aungaung@example.com",
                "09123456789",
                "Aung Aung",
                "09123456789",
                "No. 45, Bogyoke Road",
                "Room 3",
                "Kyauktada",
                "Yangon",
                "Yangon Region",
                "No. 45, Bogyoke Road, Room 3, Kyauktada, Yangon, Yangon Region",
                201L,
                "KBZ_PAY",
                "SUCCESS",
                "TXN-KBZ-987654",
                LocalDateTime.of(2026, 10, 10, 14, 35),
                new BigDecimal("153000.00"),
                301L,
                "Ninja Van Myanmar",
                "NINJA-TRACK-001",
                "DELIVERED",
                LocalDateTime.of(2026, 10, 11, 9, 0),
                LocalDateTime.of(2026, 10, 12, 10, 0)
        );

        item1 = OrderItemDetailDTO.builder()
                .orderItemId(1L)
                .productName("Wireless Noise Canceling Headphones")
                .sku("SKU-WH-001")
                .variantAttributes("{\"Color\":\"Black\"}")
                .unitPrice(new BigDecimal("100000.00"))
                .qty(1)
                .discountAmount(BigDecimal.ZERO)
                .subtotal(new BigDecimal("100000.00"))
                .build();

        item2 = OrderItemDetailDTO.builder()
                .orderItemId(2L)
                .productName("Fast Wireless Charging Pad")
                .sku("SKU-WCP-002")
                .variantAttributes("{\"Color\":\"White\"}")
                .unitPrice(new BigDecimal("25000.00"))
                .qty(2)
                .discountAmount(BigDecimal.ZERO)
                .subtotal(new BigDecimal("50000.00"))
                .build();

        reportRow1 = OrderHistoryReportDTO.builder()
                .orderId(101L)
                .orderNo("ORD-2026-0001")
                .orderDate(LocalDateTime.of(2026, 10, 10, 14, 30))
                .orderDateStr("Oct 10, 2026, 02:30 PM")
                .orderStatus("DELIVERED")
                .paymentStatus("SUCCESS")
                .paymentMethod("KBZ_PAY")
                .itemsSummary("Wireless Headphones x1, Charging Pad x2")
                .totalItemQuantity(3)
                .subtotalAmount(new BigDecimal("150000.00"))
                .shippingFee(new BigDecimal("3000.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("153000.00"))
                .customerName("Aung Aung")
                .customerEmail("aungaung@example.com")
                .customerPhone("09123456789")
                .build();

        reportRow2 = OrderHistoryReportDTO.builder()
                .orderId(102L)
                .orderNo("ORD-2026-0002")
                .orderDate(LocalDateTime.of(2026, 10, 15, 11, 0))
                .orderDateStr("Oct 15, 2026, 11:00 AM")
                .orderStatus("PROCESSING")
                .paymentStatus("SUCCESS")
                .paymentMethod("WAVE_PAY")
                .itemsSummary("USB-C Fast Cable x2")
                .totalItemQuantity(2)
                .subtotalAmount(new BigDecimal("20000.00"))
                .shippingFee(new BigDecimal("2000.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("22000.00"))
                .customerName("Aung Aung")
                .customerEmail("aungaung@example.com")
                .customerPhone("09123456789")
                .build();
    }

    // =========================================================================
    // 1. exportInvoicePdf Tests
    // =========================================================================

    @Test
    @DisplayName("Should generate valid PDF byte array for single order invoice receipt")
    void exportInvoicePdf_Success_GeneratesValidPdfBytes() {
        InvoiceRawResult rawResult = new InvoiceRawResult(
                Optional.of(completeHeader),
                List.of(item1, item2)
        );
        when(repository.callGetOrderInvoiceDetail(101L, 1L)).thenReturn(rawResult);

        byte[] pdfBytes = orderInspectionService.exportInvoicePdf(101L, 1L);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF should contain valid binary data");

        // Verify PDF Magic Header (%PDF-)
        String magicHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", magicHeader);
    }

    @Test
    @DisplayName("Should generate invoice PDF when payment and shipment info are absent")
    void exportInvoicePdf_Success_WhenPaymentAndShipmentAreNull() {
        InvoiceHeaderRow pendingHeader = new InvoiceHeaderRow(
                103L, "ORD-2026-0003", "PENDING", "PENDING",
                LocalDateTime.now(), null,
                new BigDecimal("50000.00"), new BigDecimal("2500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("52500.00"),
                1L, "Aung Aung", "aungaung@example.com", "09123456789",
                "Aung Aung", "09123456789", "Main Street", null,
                "Kamayut", "Yangon", "Yangon Region", "Main Street, Kamayut, Yangon",
                null, null, null, null, null, null,
                null, null, null, null, null, null
        );

        InvoiceRawResult rawResult = new InvoiceRawResult(
                Optional.of(pendingHeader),
                List.of(item1)
        );
        when(repository.callGetOrderInvoiceDetail(103L, 1L)).thenReturn(rawResult);

        byte[] pdfBytes = orderInspectionService.exportInvoicePdf(103L, 1L);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        String magicHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", magicHeader);
    }

    // =========================================================================
    // 2. exportOrderHistoryPdf Tests (Period, Filters, Stored Procedure)
    // =========================================================================

    @Test
    @DisplayName("Should generate valid customer order history report PDF with filtered orders")
    void exportOrderHistoryPdf_Success_WithMultipleOrders() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);

        when(repository.callGetCustomerOrderHistoryReport(1L, start, end, "DELIVERED", null))
                .thenReturn(List.of(reportRow1, reportRow2));

        byte[] pdfBytes = orderInspectionService.exportOrderHistoryPdf(1L, start, end, "DELIVERED", null);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "Report PDF should contain valid binary stream");

        String magicHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", magicHeader);
    }

    @Test
    @DisplayName("Should generate empty report PDF when customer has no orders for the period")
    void exportOrderHistoryPdf_Success_EmptyOrders() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(repository.callGetCustomerOrderHistoryReport(1L, start, end, null, null))
                .thenReturn(Collections.emptyList());

        byte[] pdfBytes = orderInspectionService.exportOrderHistoryPdf(1L, start, end, null, null);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);

        String magicHeader = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", magicHeader);
    }

    @Test
    @DisplayName("Should handle various date filter formats properly")
    void exportOrderHistoryPdf_VariousDateFormats() {
        // Test single day range (startDate == endDate)
        LocalDate sameDate = LocalDate.of(2026, 10, 10);
        when(repository.callGetCustomerOrderHistoryReport(1L, sameDate, sameDate, null, "ORD-2026"))
                .thenReturn(List.of(reportRow1));

        byte[] pdfBytesSingle = orderInspectionService.exportOrderHistoryPdf(1L, sameDate, sameDate, null, "ORD-2026");
        assertNotNull(pdfBytesSingle);
        assertEquals("%PDF-", new String(pdfBytesSingle, 0, 5, StandardCharsets.US_ASCII));

        // Test all-time range (null, null)
        when(repository.callGetCustomerOrderHistoryReport(1L, null, null, null, null))
                .thenReturn(List.of(reportRow1, reportRow2));

        byte[] pdfBytesAllTime = orderInspectionService.exportOrderHistoryPdf(1L, null, null, null, null);
        assertNotNull(pdfBytesAllTime);
        assertEquals("%PDF-", new String(pdfBytesAllTime, 0, 5, StandardCharsets.US_ASCII));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when SP execution fails")
    void exportOrderHistoryPdf_ThrowsResourceNotFound_WhenSpFails() {
        when(repository.callGetCustomerOrderHistoryReport(anyLong(), any(), any(), any(), any()))
                .thenThrow(new DataRetrievalFailureException("Customer with ID 999 does not exist"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderInspectionService.exportOrderHistoryPdf(999L, null, null, null, null));

        assertTrue(ex.getMessage().contains("Customer with ID 999 does not exist"));
    }
}
