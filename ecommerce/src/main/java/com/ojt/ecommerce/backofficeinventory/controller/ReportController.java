package com.ojt.ecommerce.backofficeinventory.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ojt.ecommerce.backofficeinventory.service.JasperReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/backoffice/reports", "/api/v1/reports"})
@RequiredArgsConstructor
public class ReportController {

    private final JasperReportService jasperReportService;

    /**
     * Helper method to set up appropriate headers based on the requested format
     */
    private HttpHeaders getReportHeaders(String format, String reportName) {
        HttpHeaders headers = new HttpHeaders();
        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", reportName + ".xlsx");
        } else {
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", reportName + ".pdf");
        }
        return headers;
    }

    @GetMapping("/daily-product-sales")
    public ResponseEntity<byte[]> getDailyProductSalesReport(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {

        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest().body("Start date and end date are required.".getBytes());
        }
        LocalDate today = LocalDate.now();
        if (startDate.isAfter(today) || endDate.isAfter(today)) {
            return ResponseEntity.badRequest().body("Dates cannot be in the future (cannot exceed today's date).".getBytes());
        }
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body("Start date cannot be after end date.".getBytes());
        }
        if (!"pdf".equalsIgnoreCase(format) && !"excel".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().body("Unsupported export format. Supported formats: pdf, excel.".getBytes());
        }

        try {
            byte[] reportBytes = jasperReportService.generateDailyProductSalesReport(startDate, endDate, format);
            return ResponseEntity.ok()
                    .headers(getReportHeaders(format, "Daily_Product_Sales_Report"))
                    .body(reportBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<byte[]> getCategoryReport(
            @RequestParam(value = "format", defaultValue = "pdf") String format) {
        if (!"pdf".equalsIgnoreCase(format) && !"excel".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().body("Unsupported export format. Supported formats: pdf, excel.".getBytes());
        }
        try {
            byte[] reportBytes = jasperReportService.generateCategoryReport(format);
            return ResponseEntity.ok()
                    .headers(getReportHeaders(format, "Category_Summary_Report"))
                    .body(reportBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate category report: " + e.getMessage(), e);
        }
    }

    @GetMapping("/top-selling")
    public ResponseEntity<byte[]> getTopSellingProductsReport(
            @RequestParam(value = "limitCount", defaultValue = "10") Integer limitCount,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {
        if (limitCount == null || limitCount < 1 || limitCount > 100) {
            return ResponseEntity.badRequest().body("Number of products must be between 1 and 100.".getBytes());
        }
        if (!"pdf".equalsIgnoreCase(format) && !"excel".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().body("Unsupported export format. Supported formats: pdf, excel.".getBytes());
        }
        try {
            byte[] reportBytes = jasperReportService.generateTopSellingProductsReport(limitCount, format);
            return ResponseEntity.ok()
                    .headers(getReportHeaders(format, "Top_Selling_Products_Report"))
                    .body(reportBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate top selling products report: " + e.getMessage(), e);
        }
    }

    @GetMapping("/refund-summary")
    public ResponseEntity<byte[]> getRefundSummaryReport(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {

        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest().body("Start date and end date are required.".getBytes());
        }
        LocalDate today = LocalDate.now();
        if (startDate.isAfter(today) || endDate.isAfter(today)) {
            return ResponseEntity.badRequest().body("Dates cannot be in the future (cannot exceed today's date).".getBytes());
        }
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body("Start date cannot be after end date.".getBytes());
        }
        if (!"pdf".equalsIgnoreCase(format) && !"excel".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().body("Unsupported export format. Supported formats: pdf, excel.".getBytes());
        }

        try {
            byte[] reportBytes = jasperReportService.generateRefundSummaryReport(startDate, endDate, format);
            return ResponseEntity.ok()
                    .headers(getReportHeaders(format, "Refund_Summary_Report"))
                    .body(reportBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate refund summary report: " + e.getMessage(), e);
        }
    }

    @GetMapping("/low-stock")
    public ResponseEntity<byte[]> getLowStockAlertReport(
            @RequestParam(value = "format", defaultValue = "pdf") String format) {
        if (!"pdf".equalsIgnoreCase(format) && !"excel".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().body("Unsupported export format. Supported formats: pdf, excel.".getBytes());
        }
        try {
            byte[] reportBytes = jasperReportService.generateLowStockAlertReport(format);
            return ResponseEntity.ok()
                    .headers(getReportHeaders(format, "Low_Stock_Alert_Report"))
                    .body(reportBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate low stock alert report: " + e.getMessage(), e);
        }
    }

    @GetMapping({"/order-fulfillment", "/order-fulfillment/export", "/order-fulfillment/pdf"})
    public ResponseEntity<byte[]> getOrderFulfillmentReport(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {

        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest().body("Start date and end date are required.".getBytes());
        }
        LocalDate today = LocalDate.now();
        if (startDate.isAfter(today) || endDate.isAfter(today)) {
            return ResponseEntity.badRequest().body("Dates cannot be in the future (cannot exceed today's date).".getBytes());
        }
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body("Start date cannot be after end date.".getBytes());
        }
        if (!"pdf".equalsIgnoreCase(format) && !"excel".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().body("Unsupported export format. Supported formats: pdf, excel.".getBytes());
        }

        try {
            byte[] reportBytes = jasperReportService.generateOrderFulfillmentReport(startDate, endDate, status, format);
            return ResponseEntity.ok()
                    .headers(getReportHeaders(format, "Order_Fulfillment_Status_Report"))
                    .body(reportBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate order fulfillment report: " + e.getMessage(), e);
        }
    }

    @GetMapping("/products/export/pdf")
    public ResponseEntity<byte[]> exportProductsPdf(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "brandId", required = false) Long brandId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "columns", required = false) List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            columns = List.of("productName", "sku", "price", "stock", "status");
        }
        try {
            byte[] pdfBytes = jasperReportService.generateDynamicProductPdf(search, categoryId, brandId, status, columns);
            return ResponseEntity.ok()
                    .headers(getReportHeaders("pdf", "Product_List_Report"))
                    .body(pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dynamic product PDF: " + e.getMessage(), e);
        }
    }

    @GetMapping({"/products/export/excel", "/products/export/xlsx"})
    public ResponseEntity<byte[]> exportProductsExcel(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "brandId", required = false) Long brandId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "columns", required = false) List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            columns = List.of("productName", "sku", "price", "stock", "status");
        }
        try {
            byte[] excelBytes = jasperReportService.generateDynamicProductExcel(search, categoryId, brandId, status, columns);
            return ResponseEntity.ok()
                    .headers(getReportHeaders("excel", "Product_List_Report"))
                    .body(excelBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dynamic product Excel: " + e.getMessage(), e);
        }
    }


    @GetMapping("/products/export")
    public ResponseEntity<byte[]> exportProductsReport(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "brandId", required = false) Long brandId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "columns", required = false) List<String> columns,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {
        if (columns == null || columns.isEmpty()) {
            columns = List.of("productName", "sku", "price", "stock", "status");
        }
        if (!"pdf".equalsIgnoreCase(format) && !"excel".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().body("Unsupported export format. Supported formats: pdf, excel.".getBytes());
        }
        try {
            byte[] reportBytes = jasperReportService.generateDynamicProductReport(search, categoryId, brandId, status, columns, format);
            return ResponseEntity.ok()
                    .headers(getReportHeaders(format, "Product_List_Report"))
                    .body(reportBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dynamic product report: " + e.getMessage(), e);
        }
    }

    @GetMapping("/brands/export/pdf")
    public ResponseEntity<byte[]> exportBrandsPdf(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "columns", required = false) List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            columns = List.of("brandName", "categoryName", "description", "status");
        }
        try {
            byte[] pdfBytes = jasperReportService.generateDynamicBrandPdf(search, status, columns);
            return ResponseEntity.ok()
                    .headers(getReportHeaders("pdf", "Brand_Management_Report"))
                    .body(pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dynamic brand PDF: " + e.getMessage(), e);
        }
    }

    @GetMapping({"/brands/export/excel", "/brands/export/xlsx"})
    public ResponseEntity<byte[]> exportBrandsExcel(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "columns", required = false) List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            columns = List.of("brandName", "categoryName", "description", "status");
        }
        try {
            byte[] excelBytes = jasperReportService.generateDynamicBrandExcel(search, status, columns);
            return ResponseEntity.ok()
                    .headers(getReportHeaders("excel", "Brand_Management_Report"))
                    .body(excelBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dynamic brand Excel: " + e.getMessage(), e);
        }
    }

    @GetMapping("/brands/export")
    public ResponseEntity<byte[]> exportBrandsReport(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "columns", required = false) List<String> columns,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {
        if (columns == null || columns.isEmpty()) {
            columns = List.of("brandName", "categoryName", "description", "status");
        }
        if (!"pdf".equalsIgnoreCase(format) && !"excel".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().body("Unsupported export format. Supported formats: pdf, excel.".getBytes());
        }
        try {
            byte[] reportBytes = jasperReportService.generateDynamicBrandReport(search, status, columns, format);
            return ResponseEntity.ok()
                    .headers(getReportHeaders(format, "Brand_Management_Report"))
                    .body(reportBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dynamic brand report: " + e.getMessage(), e);
        }
    }

    @GetMapping("/refunds/export/pdf")
    public ResponseEntity<byte[]> exportRefundsPdf(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "columns", required = false) List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            columns = List.of("refundId", "orderNo", "customerName", "refundAmount", "reason", "status", "requestedDate");
        }
        try {
            byte[] pdfBytes = jasperReportService.generateDynamicRefundPdf(startDate, endDate, search, status, columns);
            return ResponseEntity.ok()
                    .headers(getReportHeaders("pdf", "Refund_Management_Report"))
                    .body(pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dynamic refund PDF: " + e.getMessage(), e);
        }
    }

    @GetMapping({"/refunds/export/excel", "/refunds/export/xlsx"})
    public ResponseEntity<byte[]> exportRefundsExcel(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "columns", required = false) List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            columns = List.of("refundId", "orderNo", "customerName", "refundAmount", "reason", "status", "requestedDate");
        }
        try {
            byte[] excelBytes = jasperReportService.generateDynamicRefundExcel(startDate, endDate, search, status, columns);
            return ResponseEntity.ok()
                    .headers(getReportHeaders("excel", "Refund_Management_Report"))
                    .body(excelBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dynamic refund Excel: " + e.getMessage(), e);
        }
    }

    @GetMapping("/refunds/export")
    public ResponseEntity<byte[]> exportRefundsReport(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "columns", required = false) List<String> columns,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {
        if (columns == null || columns.isEmpty()) {
            columns = List.of("refundId", "orderNo", "customerName", "refundAmount", "reason", "status", "requestedDate");
        }
        if (!"pdf".equalsIgnoreCase(format) && !"excel".equalsIgnoreCase(format) && !"xlsx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().body("Unsupported export format. Supported formats: pdf, excel.".getBytes());
        }
        try {
            byte[] reportBytes = jasperReportService.generateDynamicRefundReport(startDate, endDate, search, status, columns, format);
            return ResponseEntity.ok()
                    .headers(getReportHeaders(format, "Refund_Management_Report"))
                    .body(reportBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dynamic refund report: " + e.getMessage(), e);
        }
    }
}
