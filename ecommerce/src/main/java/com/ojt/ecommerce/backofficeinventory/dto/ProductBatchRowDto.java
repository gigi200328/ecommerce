package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBatchRowDto {
    private String productName;
    private String description;
    private String categoryName;
    private String brandName;
    private BigDecimal sellingPrice;
    private BigDecimal costPrice;
    private Integer stock;
    private String sku;
    private String size;
    private String color;
    private String status;
}
