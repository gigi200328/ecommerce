package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ojt.ecommerce.backofficeinventory.dto.BrandExportDTO;
import com.ojt.ecommerce.backofficeinventory.dto.ProductCatalogExportDTO;
import com.ojt.ecommerce.backofficeinventory.repository.ReportRepository;

@ExtendWith(MockitoExtension.class)
public class JasperReportServiceDynamicExportTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private JasperReportService jasperReportService;

    private static class TestProductDTO implements ProductCatalogExportDTO {
        private final String productName;
        private final String sku;
        private final BigDecimal price;
        private final Integer stock;
        private final String status;

        TestProductDTO(String productName, String sku, BigDecimal price, Integer stock, String status) {
            this.productName = productName;
            this.sku = sku;
            this.price = price;
            this.stock = stock;
            this.status = status;
        }

        @Override public String getProductName() { return productName; }
        @Override public String getSku() { return sku; }
        @Override public BigDecimal getPrice() { return price; }
        @Override public Integer getStock() { return stock; }
        @Override public String getStatus() { return status; }
    }

    @Test
    void testGenerateDynamicProductPdf_EmptyData() throws Exception {
        when(reportRepository.getProductCatalogExport(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        byte[] pdfBytes = jasperReportService.generateDynamicProductPdf(null, null, null, null, List.of("productName", "sku", "price", "stock", "status"));
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF should contain header, title, and styling even when data is empty");
    }

    @Test
    void testGenerateDynamicProductPdf_WithData() throws Exception {
        List<ProductCatalogExportDTO> mockData = List.of(
                new TestProductDTO("iPhone 15 Pro", "IPHONE-15", new BigDecimal("999.00"), 50, "ACTIVE"),
                new TestProductDTO("MacBook Pro", "MBP-14", new BigDecimal("1999.00"), 20, "ACTIVE")
        );
        when(reportRepository.getProductCatalogExport(any(), any(), any(), any()))
                .thenReturn(mockData);

        byte[] pdfBytes = jasperReportService.generateDynamicProductPdf("phone", 1L, null, "ACTIVE", List.of("productName", "price", "stock"));
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 1000, "PDF with data rows should be generated");
    }

    @Test
    void testGenerateDynamicProductExcel_WithData() throws Exception {
        List<ProductCatalogExportDTO> mockData = List.of(
                new TestProductDTO("iPhone 15 Pro", "IPHONE-15", new BigDecimal("999.00"), 50, "ACTIVE")
        );
        when(reportRepository.getProductCatalogExport(any(), any(), any(), any()))
                .thenReturn(mockData);

        byte[] excelBytes = jasperReportService.generateDynamicProductExcel(null, null, null, null, null);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 1000, "Excel output should be generated");
    }

    private static class TestBrandDTO implements BrandExportDTO {
        private final String brandName;
        private final String categoryName;
        private final String description;
        private final String status;

        TestBrandDTO(String brandName, String categoryName, String description, String status) {
            this.brandName = brandName;
            this.categoryName = categoryName;
            this.description = description;
            this.status = status;
        }

        @Override public String getBrandName() { return brandName; }
        @Override public String getCategoryName() { return categoryName; }
        @Override public String getDescription() { return description; }
        @Override public String getStatus() { return status; }
    }

    @Test
    void testGenerateDynamicBrandPdf_EmptyData() throws Exception {
        when(reportRepository.getBrandExportReport(any(), any()))
                .thenReturn(Collections.emptyList());

        byte[] pdfBytes = jasperReportService.generateDynamicBrandPdf(null, "ALL", List.of("brandName", "categoryName", "description", "status"));
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "Brand PDF should contain header and title even when data is empty");
    }

    @Test
    void testGenerateDynamicBrandPdf_WithData() throws Exception {
        List<BrandExportDTO> mockData = List.of(
                new TestBrandDTO("Apple", "Electronics", "Apple Inc. Devices", "ACTIVE"),
                new TestBrandDTO("Samsung", "Electronics", "Samsung Galaxy Products", "ACTIVE")
        );
        when(reportRepository.getBrandExportReport(any(), any()))
                .thenReturn(mockData);

        byte[] pdfBytes = jasperReportService.generateDynamicBrandPdf("apple", "ACTIVE", List.of("brandName", "description", "status"));
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 1000, "Brand PDF with data rows should be generated");
    }

    @Test
    void testGenerateDynamicBrandExcel_WithData() throws Exception {
        List<BrandExportDTO> mockData = List.of(
                new TestBrandDTO("Apple", "Electronics", "Apple Inc. Devices", "ACTIVE")
        );
        when(reportRepository.getBrandExportReport(any(), any()))
                .thenReturn(mockData);

        byte[] excelBytes = jasperReportService.generateDynamicBrandExcel(null, "ALL", null);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 1000, "Brand Excel output should be generated");
    }

    @Test
    void testGenerateCategoryReport_EmptyData() throws Exception {
        when(reportRepository.getCategorySummary()).thenReturn(Collections.emptyList());
        byte[] pdfBytes = jasperReportService.generateCategoryReport("pdf");
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
    }

    @Test
    void testGenerateDailyProductSalesReport_EmptyData() throws Exception {
        when(reportRepository.getDailyProductSales(any(), any())).thenReturn(Collections.emptyList());
        byte[] pdfBytes = jasperReportService.generateDailyProductSalesReport(
                java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now(), "pdf");
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
    }

    @Test
    void testGenerateLowStockAlertReport_EmptyData() throws Exception {
        when(reportRepository.getLowStockAlertReport()).thenReturn(Collections.emptyList());
        byte[] pdfBytes = jasperReportService.generateLowStockAlertReport("pdf");
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
    }

    @Test
    void testGenerateOrderFulfillmentReport_EmptyData() throws Exception {
        when(reportRepository.getOrderFulfillmentReport(any(), any(), any())).thenReturn(Collections.emptyList());
        byte[] pdfBytes = jasperReportService.generateOrderFulfillmentReport(
                java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now(), "ALL", "pdf");
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
    }

    @Test
    void testGenerateRefundSummaryReport_EmptyData() throws Exception {
        when(reportRepository.getRefundSummaryReport(any(), any())).thenReturn(Collections.emptyList());
        byte[] pdfBytes = jasperReportService.generateRefundSummaryReport(
                java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now(), "pdf");
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
    }

    @Test
    void testGenerateTopSellingProductsReport_EmptyData() throws Exception {
        when(reportRepository.getTopSellingProducts(any())).thenReturn(Collections.emptyList());
        byte[] pdfBytes = jasperReportService.generateTopSellingProductsReport(10, "pdf");
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
    }

    private static class TestRefundExportDTO implements com.ojt.ecommerce.backofficeinventory.dto.RefundExportDTO {
        private final Long refundId;
        private final String orderNo;
        private final String customerName;
        private final BigDecimal refundAmount;
        private final String reason;
        private final String status;
        private final String requestedDate;

        TestRefundExportDTO(Long refundId, String orderNo, String customerName, BigDecimal refundAmount, String reason, String status, String requestedDate) {
            this.refundId = refundId;
            this.orderNo = orderNo;
            this.customerName = customerName;
            this.refundAmount = refundAmount;
            this.reason = reason;
            this.status = status;
            this.requestedDate = requestedDate;
        }

        @Override public Long getRefundId() { return refundId; }
        @Override public String getOrderNo() { return orderNo; }
        @Override public String getCustomerName() { return customerName; }
        @Override public BigDecimal getRefundAmount() { return refundAmount; }
        @Override public String getReason() { return reason; }
        @Override public String getStatus() { return status; }
        @Override public String getRequestedDate() { return requestedDate; }
    }

    @Test
    void testGenerateDynamicRefundPdf_WithData() throws Exception {
        when(reportRepository.getRefundExportReport(any(), any(), any(), any()))
                .thenReturn(List.of(
                        new TestRefundExportDTO(1L, "ORD-1001", "Aung Aung", BigDecimal.valueOf(50.0), "DAMAGED_ITEM", "PENDING", "2026-10-18"),
                        new TestRefundExportDTO(2L, "ORD-1002", "Su Su", BigDecimal.valueOf(100.0), "DEFECTIVE_PRODUCT", "APPROVED", "2026-10-17")
                ));

        byte[] pdfBytes = jasperReportService.generateDynamicRefundPdf(
                java.time.LocalDate.now().minusDays(7),
                java.time.LocalDate.now(),
                null,
                "ALL",
                List.of("refundId", "orderNo", "customerName", "refundAmount", "reason", "status", "requestedDate")
        );
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
    }

    @Test
    void testGenerateDynamicRefundExcel_WithData() throws Exception {
        when(reportRepository.getRefundExportReport(any(), any(), any(), any()))
                .thenReturn(List.of(
                        new TestRefundExportDTO(1L, "ORD-1001", "Aung Aung", BigDecimal.valueOf(50.0), "DAMAGED_ITEM", "PENDING", "2026-10-18")
                ));

        byte[] excelBytes = jasperReportService.generateDynamicRefundExcel(
                java.time.LocalDate.now().minusDays(7),
                java.time.LocalDate.now(),
                null,
                "ALL",
                List.of("refundId", "orderNo", "customerName", "refundAmount", "status")
        );
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 500);
    }

    @Test
    void testGenerateDynamicRefundPdf_EmptyData() throws Exception {
        when(reportRepository.getRefundExportReport(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        byte[] pdfBytes = jasperReportService.generateDynamicRefundPdf(
                null, null, null, "ALL", null
        );
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
    }

    private static class TestInventoryExportDTO implements com.ojt.ecommerce.backofficeinventory.dto.InventoryExportDTO {
        private final String variantAndSku;
        private final String product;
        private final BigDecimal sellingPrice;
        private final Integer physicalStock;
        private final Integer reservedStock;
        private final Integer availableStock;
        private final Integer reorderLevel;
        private final String expiryAndBatch;
        private final String status;

        TestInventoryExportDTO(String variantAndSku, String product, BigDecimal sellingPrice,
                               Integer physicalStock, Integer reservedStock, Integer availableStock,
                               Integer reorderLevel, String expiryAndBatch, String status) {
            this.variantAndSku = variantAndSku;
            this.product = product;
            this.sellingPrice = sellingPrice;
            this.physicalStock = physicalStock;
            this.reservedStock = reservedStock;
            this.availableStock = availableStock;
            this.reorderLevel = reorderLevel;
            this.expiryAndBatch = expiryAndBatch;
            this.status = status;
        }

        @Override public String getVariantAndSku() { return variantAndSku; }
        @Override public String getProduct() { return product; }
        @Override public BigDecimal getSellingPrice() { return sellingPrice; }
        @Override public Integer getPhysicalStock() { return physicalStock; }
        @Override public Integer getReservedStock() { return reservedStock; }
        @Override public Integer getAvailableStock() { return availableStock; }
        @Override public Integer getReorderLevel() { return reorderLevel; }
        @Override public String getExpiryAndBatch() { return expiryAndBatch; }
        @Override public String getStatus() { return status; }
    }

    @Test
    void testGenerateDynamicInventoryPdf_WithData() throws Exception {
        when(reportRepository.getInventoryExportReport(any(), any(), any()))
                .thenReturn(List.of(
                        new TestInventoryExportDTO("IPHONE15-PRO (Black / 128GB)", "iPhone 15 Pro", new BigDecimal("999.00"), 45, 0, 45, 10, "2026-12-31 (BATCH-1)", "NORMAL"),
                        new TestInventoryExportDTO("MBP-16-M3", "MacBook Pro 16", new BigDecimal("2499.00"), 5, 0, 5, 10, "-", "LOW_STOCK")
                ));

        byte[] pdfBytes = jasperReportService.generateDynamicInventoryPdf(
                "ALL", null, "ALL",
                List.of("variantAndSku", "product", "sellingPrice", "physicalStock", "reservedStock", "availableStock", "reorderLevel", "expiryAndBatch", "status")
        );
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    @Test
    void testGenerateDynamicInventoryExcel_WithData() throws Exception {
        when(reportRepository.getInventoryExportReport(any(), any(), any()))
                .thenReturn(List.of(
                        new TestInventoryExportDTO("IPHONE15-PRO (Black / 128GB)", "iPhone 15 Pro", new BigDecimal("999.00"), 45, 0, 45, 10, "2026-12-31 (BATCH-1)", "NORMAL")
                ));

        byte[] excelBytes = jasperReportService.generateDynamicInventoryExcel(
                "LOW_STOCK", "phone", "LOW_STOCK",
                List.of("variantAndSku", "product", "sellingPrice", "physicalStock", "status")
        );
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 500);
        assertEquals("PK", new String(excelBytes, 0, 2));
    }

    @Test
    void testGenerateDynamicInventoryPdf_ExpiringSoonTab_EmptyData() throws Exception {
        when(reportRepository.getInventoryExportReport(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        byte[] pdfBytes = jasperReportService.generateDynamicInventoryPdf(
                "EXPIRING_SOON", null, "ALL", null
        );
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    private static class TestOrderPageExportDTO implements com.ojt.ecommerce.backofficeinventory.dto.OrderPageExportDTO {
        private final String orderNo;
        private final String orderDate;
        private final String customerName;
        private final BigDecimal totalAmount;
        private final String orderStatus;
        private final String paymentStatus;

        TestOrderPageExportDTO(String orderNo, String orderDate, String customerName, BigDecimal totalAmount, String orderStatus, String paymentStatus) {
            this.orderNo = orderNo;
            this.orderDate = orderDate;
            this.customerName = customerName;
            this.totalAmount = totalAmount;
            this.orderStatus = orderStatus;
            this.paymentStatus = paymentStatus;
        }

        @Override public String getOrderNo() { return orderNo; }
        @Override public String getOrderDate() { return orderDate; }
        @Override public String getCustomerName() { return customerName; }
        @Override public BigDecimal getTotalAmount() { return totalAmount; }
        @Override public String getOrderStatus() { return orderStatus; }
        @Override public String getPaymentStatus() { return paymentStatus; }
    }

    @Test
    void testGenerateDynamicOrdersPagePdf_WithData() throws Exception {
        when(reportRepository.getOrdersPageExportReport(any(), any()))
                .thenReturn(List.of(
                        new TestOrderPageExportDTO("ORD-20261019-005", "2026-10-18", "Min Khant Ko", new BigDecimal("2499.00"), "DELIVERED", "REFUNDED"),
                        new TestOrderPageExportDTO("ORD-20261016-002", "2026-10-16", "Su Myat Noe", new BigDecimal("2499.00"), "DELIVERED", "SUCCESS")
                ));

        byte[] pdfBytes = jasperReportService.generateDynamicOrdersPagePdf(
                "ALL", null,
                List.of("orderNo", "orderDate", "customerName", "totalAmount", "orderStatus", "paymentStatus")
        );
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    @Test
    void testGenerateDynamicOrdersPageExcel_WithData() throws Exception {
        when(reportRepository.getOrdersPageExportReport(any(), any()))
                .thenReturn(List.of(
                        new TestOrderPageExportDTO("ORD-20261019-005", "2026-10-18", "Min Khant Ko", new BigDecimal("2499.00"), "DELIVERED", "REFUNDED")
                ));

        byte[] excelBytes = jasperReportService.generateDynamicOrdersPageExcel(
                "DELIVERED", "Min Khant",
                List.of("orderNo", "orderDate", "customerName", "totalAmount")
        );
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 500);
        assertEquals("PK", new String(excelBytes, 0, 2));
    }

    @Test
    void testGenerateDynamicOrdersPagePdf_EmptyData() throws Exception {
        when(reportRepository.getOrdersPageExportReport(any(), any()))
                .thenReturn(Collections.emptyList());

        byte[] pdfBytes = jasperReportService.generateDynamicOrdersPagePdf(
                "PENDING", null, null
        );
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }
}
