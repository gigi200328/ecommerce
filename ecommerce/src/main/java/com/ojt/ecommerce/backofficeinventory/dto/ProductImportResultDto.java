package com.ojt.ecommerce.backofficeinventory.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportResultDto {
    private int totalRows;
    private int successCount;
    private int failedCount;
    private List<RowError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int rowNumber;
        private String productName;
        private String sku;
        private String errorReason;
    }
}
