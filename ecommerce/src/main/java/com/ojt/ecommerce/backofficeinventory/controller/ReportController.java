package com.ojt.ecommerce.backofficeinventory.controller;

import java.time.LocalDate;

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
@RequestMapping("/api/v1/reports")
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
        try {
            byte[] reportBytes = jasperReportService.generateLowStockAlertReport(format);
            return ResponseEntity.ok()
                    .headers(getReportHeaders(format, "Low_Stock_Alert_Report"))
                    .body(reportBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate low stock alert report: " + e.getMessage(), e);
        }
    }
}
