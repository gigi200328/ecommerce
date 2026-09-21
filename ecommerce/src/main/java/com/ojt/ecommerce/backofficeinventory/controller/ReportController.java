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

    @GetMapping("/daily-product-sales/pdf")
    public ResponseEntity<byte[]> getDailyProductSalesPdf(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Generate the PDF as a byte array
            byte[] pdfBytes = jasperReportService.generateDailyProductSalesPdf(startDate, endDate);

            // Set up HTTP headers for file download/inline viewing
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "Daily_Product_Sales_Report.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            // In a real application, you might want a custom exception here.
            // The GlobalExceptionHandler will catch this RuntimeException and return the standard ErrorResponse.
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
        }
    }
}
