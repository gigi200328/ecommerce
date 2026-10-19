package com.ojt.ecommerce.backofficeinventory.service;

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
}
