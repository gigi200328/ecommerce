package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyProfitComparisonDto {

    private Integer year;
    private BigDecimal annualRevenue;
    private BigDecimal annualCost;
    private BigDecimal annualGrossProfit;
    private Double annualMarginPercentage;
    private String bestPerformingMonth;

    private List<MonthlyDetail> monthlyDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyDetail {
        private Integer month; // 1-12
        private String monthName; // "January", "February", etc.
        private Integer orderCount;
        private Integer unitsSold;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal grossProfit;
        private Double marginPercentage;

        // Comparison metrics with previous month
        private BigDecimal prevMonthRevenue;
        private BigDecimal prevMonthProfit;
        private Double revenueGrowthPercentage; // e.g. +15.5% or -5.2%
        private Double profitGrowthPercentage;  // e.g. +22.0%
        private Double marginChangePoints;      // e.g. +2.5% points
        private String trend; // "UP", "DOWN", "EQUAL", "N/A"

        private List<TopVariantProfit> topVariants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopVariantProfit {
        private Long variantId;
        private String productName;
        private String sku;
        private Integer qtySold;
        private BigDecimal unitPrice;
        private BigDecimal costPrice;
        private BigDecimal totalRevenue;
        private BigDecimal totalProfit;
        private Double marginPercentage;
    }
}
