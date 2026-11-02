package com.ojt.ecommerce.backofficeinventory.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ojt.ecommerce.backofficeinventory.dto.ProductImportResultDto;
import com.ojt.ecommerce.backofficeinventory.service.ProductExcelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/backoffice/products")
@RequiredArgsConstructor
@Tag(name = "Product Excel Import & Template", description = "Back-Office APIs for Bulk Product Import via Excel")
public class ProductExcelController {

    private final ProductExcelService productExcelService;

    @Operation(summary = "Download Product Import Excel Template (.xlsx) with pre-filled category/brand dropdowns")
    @GetMapping("/excel-template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] excelBytes = productExcelService.generateExcelTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"products_import_template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @Operation(summary = "Bulk import products from uploaded Excel (.xlsx) file")
    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImportResultDto> bulkImportProducts(@RequestParam("file") MultipartFile file) {
        log.info("Received Excel bulk import file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());
        ProductImportResultDto result = productExcelService.importProductsFromExcel(file);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Batch create products directly from interactive Web Excel Grid")
    @PostMapping("/batch-create")
    public ResponseEntity<ProductImportResultDto> batchCreateProducts(
            @org.springframework.web.bind.annotation.RequestBody java.util.List<com.ojt.ecommerce.backofficeinventory.dto.ProductBatchRowDto> rows) {
        log.info("Received interactive Web Excel Grid batch creation with {} rows", rows.size());
        ProductImportResultDto result = productExcelService.batchCreateProducts(rows);
        return ResponseEntity.ok(result);
    }
}
