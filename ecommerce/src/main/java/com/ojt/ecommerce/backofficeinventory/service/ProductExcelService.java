package com.ojt.ecommerce.backofficeinventory.service;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;
import com.ojt.ecommerce.backofficeinventory.dto.ProductImportResultDto;

public interface ProductExcelService {

    /**
     * Generates a pre-formatted Excel template (.xlsx) with column validations
     * and dropdown lookups for Category, Brand, and Status.
     */
    byte[] generateExcelTemplate() throws IOException;

    /**
     * Parses an uploaded Excel file and batch-imports products, variants,
     * attribute options, and inventory records.
     */
    ProductImportResultDto importProductsFromExcel(MultipartFile file);

    /**
     * Batch-creates products from interactive Web Excel Grid rows.
     */
    ProductImportResultDto batchCreateProducts(java.util.List<com.ojt.ecommerce.backofficeinventory.dto.ProductBatchRowDto> rows);
}
