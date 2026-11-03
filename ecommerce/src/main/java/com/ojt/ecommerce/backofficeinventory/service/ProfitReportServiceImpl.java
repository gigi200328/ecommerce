package com.ojt.ecommerce.backofficeinventory.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.MonthlyProfitComparisonDto;
import com.ojt.ecommerce.backofficeinventory.dto.MonthlyProfitComparisonDto.MonthlyDetail;
import com.ojt.ecommerce.backofficeinventory.dto.MonthlyProfitComparisonDto.TopVariantProfit;
import com.ojt.ecommerce.backofficeinventory.repository.OrderRepository;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitReportServiceImpl implements ProfitReportService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public MonthlyProfitComparisonDto getMonthOnMonthProfitReport(Integer year) {
        int targetYear = (year != null && year >= 2000 && year <= 2100) ? year : LocalDate.now().getYear();

        LocalDateTime startOfYear = LocalDateTime.of(targetYear, 1, 1, 0, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(targetYear, 12, 31, 23, 59, 59);

        List<Order> orders = orderRepository.findWithItemsByCreatedAtBetween(startOfYear, endOfYear);

        // Group valid orders by month (1 - 12)
        Map<Integer, List<Order>> ordersByMonth = orders.stream()
                .filter(o -> o.getOrderStatus() == null || !"CANCELLED".equalsIgnoreCase(o.getOrderStatus()))
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().getMonthValue()));

        List<MonthlyDetail> monthlyDetails = new ArrayList<>();

        BigDecimal annualRevenue = BigDecimal.ZERO;
        BigDecimal annualCost = BigDecimal.ZERO;
        BigDecimal annualProfit = BigDecimal.ZERO;

        BigDecimal prevMonthRevenue = BigDecimal.ZERO;
        BigDecimal prevMonthProfit = BigDecimal.ZERO;
        Double prevMonthMargin = 0.0;

        String bestMonthName = "N/A";
        BigDecimal maxProfit = BigDecimal.valueOf(-1);

        for (int m = 1; m <= 12; m++) {
            List<Order> monthOrders = ordersByMonth.getOrDefault(m, Collections.emptyList());
            String monthName = Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            int orderCount = monthOrders.size();
            int unitsSold = 0;
            BigDecimal revenue = BigDecimal.ZERO;
            BigDecimal cost = BigDecimal.ZERO;

            Map<Long, TopVariantProfit> variantStats = new HashMap<>();

            for (Order o : monthOrders) {
                if (o.getOrderItems() != null) {
                    for (OrderItem item : o.getOrderItems()) {
                        int q = item.getQty() != null ? item.getQty() : 0;
                        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                        BigDecimal costPrice = item.getCostPrice() != null ? item.getCostPrice() : 
                                (item.getVariant() != null && item.getVariant().getCostPrice() != null ? item.getVariant().getCostPrice() : BigDecimal.ZERO);

                        BigDecimal itemRevenue = unitPrice.multiply(BigDecimal.valueOf(q));
                        BigDecimal itemCost = costPrice.multiply(BigDecimal.valueOf(q));

                        unitsSold += q;
                        revenue = revenue.add(itemRevenue);
                        cost = cost.add(itemCost);

                        if (item.getVariant() != null && item.getVariant().getVariantId() != null) {
                            Long vId = item.getVariant().getVariantId();
                            TopVariantProfit curr = variantStats.getOrDefault(vId, TopVariantProfit.builder()
                                    .variantId(vId)
                                    .productName(item.getProductName() != null ? item.getProductName() : (item.getVariant().getProduct() != null ? item.getVariant().getProduct().getProductName() : "Product"))
                                    .sku(item.getVariant().getSku())
                                    .qtySold(0)
                                    .unitPrice(unitPrice)
                                    .costPrice(costPrice)
                                    .totalRevenue(BigDecimal.ZERO)
                                    .totalProfit(BigDecimal.ZERO)
                                    .marginPercentage(0.0)
                                    .build());

                            int updatedQty = curr.getQtySold() + q;
                            BigDecimal updatedRev = curr.getTotalRevenue().add(itemRevenue);
                            BigDecimal updatedProfit = curr.getTotalProfit().add(itemRevenue.subtract(itemCost));
                            double vMargin = updatedRev.compareTo(BigDecimal.ZERO) > 0 ?
                                    updatedProfit.multiply(BigDecimal.valueOf(100)).divide(updatedRev, 2, RoundingMode.HALF_UP).doubleValue() : 0.0;

                            curr.setQtySold(updatedQty);
                            curr.setTotalRevenue(updatedRev);
                            curr.setTotalProfit(updatedProfit);
                            curr.setMarginPercentage(vMargin);

                            variantStats.put(vId, curr);
                        }
                    }
                }
            }

            BigDecimal grossProfit = revenue.subtract(cost);
            double marginPercentage = revenue.compareTo(BigDecimal.ZERO) > 0 ?
                    grossProfit.multiply(BigDecimal.valueOf(100)).divide(revenue, 2, RoundingMode.HALF_UP).doubleValue() : 0.0;

            // MoM Comparison with previous month
            Double revenueGrowth = null;
            Double profitGrowth = null;
            Double marginChange = null;
            String trend = "EQUAL";

            if (m > 1) {
                if (prevMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    revenueGrowth = revenue.subtract(prevMonthRevenue)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(prevMonthRevenue, 2, RoundingMode.HALF_UP)
                            .doubleValue();
                } else if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                    revenueGrowth = 100.0;
                }

                if (prevMonthProfit.compareTo(BigDecimal.ZERO) > 0) {
                    profitGrowth = grossProfit.subtract(prevMonthProfit)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(prevMonthProfit, 2, RoundingMode.HALF_UP)
                            .doubleValue();
                } else if (grossProfit.compareTo(BigDecimal.ZERO) > 0) {
                    profitGrowth = 100.0;
                }

                marginChange = Math.round((marginPercentage - prevMonthMargin) * 100.0) / 100.0;

                if (profitGrowth != null) {
                    trend = profitGrowth > 0 ? "UP" : (profitGrowth < 0 ? "DOWN" : "EQUAL");
                }
            } else {
                trend = "N/A";
            }

            // Top 3 variants by profit
            List<TopVariantProfit> topVariants = variantStats.values().stream()
                    .sorted((a, b) -> b.getTotalProfit().compareTo(a.getTotalProfit()))
                    .limit(3)
                    .collect(Collectors.toList());

            monthlyDetails.add(MonthlyDetail.builder()
                    .month(m)
                    .monthName(monthName)
                    .orderCount(orderCount)
                    .unitsSold(unitsSold)
                    .revenue(revenue)
                    .cost(cost)
                    .grossProfit(grossProfit)
                    .marginPercentage(marginPercentage)
                    .prevMonthRevenue(prevMonthRevenue)
                    .prevMonthProfit(prevMonthProfit)
                    .revenueGrowthPercentage(revenueGrowth)
                    .profitGrowthPercentage(profitGrowth)
                    .marginChangePoints(marginChange)
                    .trend(trend)
                    .topVariants(topVariants)
                    .build());

            annualRevenue = annualRevenue.add(revenue);
            annualCost = annualCost.add(cost);
            annualProfit = annualProfit.add(grossProfit);

            if (grossProfit.compareTo(maxProfit) > 0 && orderCount > 0) {
                maxProfit = grossProfit;
                bestMonthName = monthName;
            }

            prevMonthRevenue = revenue;
            prevMonthProfit = grossProfit;
            prevMonthMargin = marginPercentage;
        }

        double annualMargin = annualRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                annualProfit.multiply(BigDecimal.valueOf(100)).divide(annualRevenue, 2, RoundingMode.HALF_UP).doubleValue() : 0.0;

        return MonthlyProfitComparisonDto.builder()
                .year(targetYear)
                .annualRevenue(annualRevenue)
                .annualCost(annualCost)
                .annualGrossProfit(annualProfit)
                .annualMarginPercentage(annualMargin)
                .bestPerformingMonth(bestMonthName)
                .monthlyDetails(monthlyDetails)
                .build();
    }

    @Override
    public byte[] exportMonthOnMonthProfitExcel(Integer year) {
        MonthlyProfitComparisonDto report = getMonthOnMonthProfitReport(year);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Profit & Margin " + report.getYear());

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Title Row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Month-on-Month Profit & Margin Comparison Report (" + report.getYear() + ")");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Summary Row
            Row sumRow = sheet.createRow(2);
            sumRow.createCell(0).setCellValue("Annual Revenue: " + String.format("%,.2f MMK", report.getAnnualRevenue()));
            sumRow.createCell(3).setCellValue("Annual Cost: " + String.format("%,.2f MMK", report.getAnnualCost()));
            sumRow.createCell(6).setCellValue("Annual Profit: " + String.format("%,.2f MMK", report.getAnnualGrossProfit()));
            sumRow.createCell(8).setCellValue("Margin: " + report.getAnnualMarginPercentage() + "%");

            // Table Headers
            String[] headers = {
                    "Month", "Orders", "Units Sold", "Revenue (MMK)", "Cost (MMK)",
                    "Gross Profit (MMK)", "Margin (%)", "MoM Rev Growth (%)", "MoM Profit Growth (%)", "Trend"
            };

            Row tableHeader = sheet.createRow(4);
            tableHeader.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = tableHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 5;
            for (MonthlyDetail d : report.getMonthlyDetails()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(d.getMonthName());
                row.createCell(1).setCellValue(d.getOrderCount());
                row.createCell(2).setCellValue(d.getUnitsSold());
                row.createCell(3).setCellValue(d.getRevenue().doubleValue());
                row.createCell(4).setCellValue(d.getCost().doubleValue());
                row.createCell(5).setCellValue(d.getGrossProfit().doubleValue());
                row.createCell(6).setCellValue(d.getMarginPercentage() + "%");
                row.createCell(7).setCellValue(d.getRevenueGrowthPercentage() != null ? d.getRevenueGrowthPercentage() + "%" : "-");
                row.createCell(8).setCellValue(d.getProfitGrowthPercentage() != null ? d.getProfitGrowthPercentage() + "%" : "-");
                row.createCell(9).setCellValue(d.getTrend());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate excel profit report", e);
            throw new RuntimeException("Excel generation error: " + e.getMessage());
        }
    }
}
