package com.ojt.ecommerce.backofficeinventory.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ojt.ecommerce.backofficeinventory.dto.BrandExportDTO;
import com.ojt.ecommerce.backofficeinventory.dto.CategorySummaryDTO;
import com.ojt.ecommerce.backofficeinventory.dto.DailyProductSalesDTO;
import com.ojt.ecommerce.backofficeinventory.dto.LowStockAlertDTO;
import com.ojt.ecommerce.backofficeinventory.dto.OrderFulfillmentReportDTO;
import com.ojt.ecommerce.backofficeinventory.dto.ProductCatalogExportDTO;
import com.ojt.ecommerce.backofficeinventory.dto.RefundSummaryReportDTO;
import com.ojt.ecommerce.backofficeinventory.dto.TopSellingProductDTO;
import com.ojt.ecommerce.backofficeinventory.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

@Service
@RequiredArgsConstructor
public class JasperReportService {

    private final ReportRepository reportRepository;

    /**
     * Helper method to export JasperPrint to either PDF or Excel.
     */
    private byte[] exportReport(JasperPrint jasperPrint, String format) throws Exception {
        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
            exporter.exportReport();
            return outputStream.toByteArray();
        } else {
            // Default to PDF
            return JasperExportManager.exportReportToPdf(jasperPrint);
        }
    }

    public byte[] generateDailyProductSalesReport(LocalDate startDate, LocalDate endDate, String format) throws Exception {
        // Fetch data from the database using the Stored Procedure
        List<DailyProductSalesDTO> salesData = reportRepository.getDailyProductSales(startDate, endDate);

        InputStream reportStream = getClass().getResourceAsStream("/reports/DailyProductSalesReport.jrxml");
        if (reportStream == null) {
            throw new RuntimeException("Report template not found in /reports/DailyProductSalesReport.jrxml");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(salesData);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Daily E-commerce Product Sales Report");
        parameters.put("START_DATE", startDate.toString());
        parameters.put("END_DATE", endDate.toString());

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        return exportReport(jasperPrint, format);
    }

    public byte[] generateCategoryReport(String format) throws Exception {
        List<CategorySummaryDTO> categoryData = reportRepository.getCategorySummary();

        InputStream reportStream = getClass().getResourceAsStream("/reports/CategorySummaryReport.jrxml");
        if (reportStream == null) {
            throw new RuntimeException("Report template not found in /reports/CategorySummaryReport.jrxml");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(categoryData);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Category Summary Report");

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        return exportReport(jasperPrint, format);
    }

    public byte[] generateTopSellingProductsReport(Integer limitCount, String format) throws Exception {
        List<TopSellingProductDTO> topSellingData = reportRepository.getTopSellingProducts(limitCount);

        InputStream reportStream = getClass().getResourceAsStream("/reports/TopSellingProductsReport.jrxml");
        if (reportStream == null) {
            throw new RuntimeException("Report template not found in /reports/TopSellingProductsReport.jrxml");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(topSellingData);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Top Selling Products Report");
        parameters.put("LIMIT_COUNT", limitCount);

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        return exportReport(jasperPrint, format);
    }

    public byte[] generateRefundSummaryReport(LocalDate startDate, LocalDate endDate, String format) throws Exception {
        List<RefundSummaryReportDTO> refundData = reportRepository.getRefundSummaryReport(startDate, endDate);

        InputStream reportStream = getClass().getResourceAsStream("/reports/RefundSummaryReport.jrxml");
        if (reportStream == null) {
            throw new RuntimeException("Report template not found in /reports/RefundSummaryReport.jrxml");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(refundData);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Refund Summary Report");
        parameters.put("START_DATE", startDate.toString());
        parameters.put("END_DATE", endDate.toString());

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        return exportReport(jasperPrint, format);
    }

    public byte[] generateLowStockAlertReport(String format) throws Exception {
        // 1. Fetch data from the database using the Stored Procedure
        List<LowStockAlertDTO> lowStockData = reportRepository.getLowStockAlertReport();

        // 2. Load and compile the Jasper Report (.jrxml) template
        InputStream reportStream = getClass().getResourceAsStream("/reports/LowStockAlertReport.jrxml");
        if (reportStream == null) {
            throw new RuntimeException("Report template not found in /reports/LowStockAlertReport.jrxml");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // 3. Map the data to a Jasper Reports Data Source
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(lowStockData);

        // 4. Set up report parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Low Stock Alert Report");

        // 5. Fill the report with data and parameters
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        // 6. Export the report to PDF or Excel using helper method
        return exportReport(jasperPrint, format);
    }

    public byte[] generateOrderFulfillmentReport(LocalDate startDate, LocalDate endDate, String statusFilter, String format) throws Exception {
        // Default statusFilter to "ALL" if null, empty, or whitespace
        if (statusFilter == null || statusFilter.trim().isEmpty()) {
            statusFilter = "ALL";
        }

        // 1. Fetch data from the database using the Stored Procedure
        List<OrderFulfillmentReportDTO> orderData = reportRepository.getOrderFulfillmentReport(startDate, endDate, statusFilter);

        // 2. Load and compile the Jasper Report (.jrxml) template
        InputStream reportStream = getClass().getResourceAsStream("/reports/OrderFulfillmentReport.jrxml");
        if (reportStream == null) {
            throw new RuntimeException("Report template not found in /reports/OrderFulfillmentReport.jrxml");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // 3. Map the data to a Jasper Reports Data Source
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(orderData);

        // 4. Set up report parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Order Fulfillment Status Report");
        parameters.put("START_DATE", startDate.toString());
        parameters.put("END_DATE", endDate.toString());
        parameters.put("STATUS_FILTER", statusFilter);

        // 5. Fill the report with data and parameters
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        // 6. Export the report to PDF or Excel using helper method
        return exportReport(jasperPrint, format);
    }

    public byte[] generateOrderFulfillmentPdf(LocalDate startDate, LocalDate endDate, String statusFilter) throws Exception {
        return generateOrderFulfillmentReport(startDate, endDate, statusFilter, "pdf");
    }

    // =========================================================================
    // DYNAMIC COLUMN PRODUCT CATALOG EXPORT (PDF & EXCEL)
    // =========================================================================

    private static class ColumnDef {
        final String key;
        final String header;
        final Class<?> valueClass;
        final HorizontalTextAlignEnum detailAlign;
        final String pattern;

        ColumnDef(String key, String header, Class<?> valueClass, HorizontalTextAlignEnum detailAlign, String pattern) {
            this.key = key;
            this.header = header;
            this.valueClass = valueClass;
            this.detailAlign = detailAlign;
            this.pattern = pattern;
        }
    }

    private static final Map<String, ColumnDef> AVAILABLE_COLUMNS = new LinkedHashMap<>();
    static {
        AVAILABLE_COLUMNS.put("productName", new ColumnDef("productName", "PRODUCT", String.class, HorizontalTextAlignEnum.LEFT, null));
        AVAILABLE_COLUMNS.put("sku", new ColumnDef("sku", "SKU", String.class, HorizontalTextAlignEnum.CENTER, null));
        AVAILABLE_COLUMNS.put("price", new ColumnDef("price", "PRICE", BigDecimal.class, HorizontalTextAlignEnum.RIGHT, "#,##0.00"));
        AVAILABLE_COLUMNS.put("stock", new ColumnDef("stock", "STOCK", Integer.class, HorizontalTextAlignEnum.CENTER, null));
        AVAILABLE_COLUMNS.put("status", new ColumnDef("status", "STATUS", String.class, HorizontalTextAlignEnum.CENTER, null));
    }

    public byte[] generateDynamicProductPdf(String search, Long categoryId, Long brandId, String status, List<String> selectedColumns) throws Exception {
        return generateDynamicProductReport(search, categoryId, brandId, status, selectedColumns, "pdf");
    }

    public byte[] generateDynamicProductExcel(String search, Long categoryId, Long brandId, String status, List<String> selectedColumns) throws Exception {
        return generateDynamicProductReport(search, categoryId, brandId, status, selectedColumns, "excel");
    }

    public static class ProductCatalogExportItem {
        private String productName;
        private String sku;
        private BigDecimal price;
        private Integer stock;
        private String status;

        public ProductCatalogExportItem(String productName, String sku, BigDecimal price, Integer stock, String status) {
            this.productName = productName != null ? productName : "N/A";
            this.sku = sku != null ? sku : "-";
            this.price = price != null ? price : BigDecimal.ZERO;
            this.stock = stock != null ? stock : 0;
            this.status = status != null ? status : "UNKNOWN";
        }

        public String getProductName() { return productName; }
        public String getSku() { return sku; }
        public BigDecimal getPrice() { return price; }
        public Integer getStock() { return stock; }
        public String getStatus() { return status; }
    }

    public byte[] generateDynamicProductReport(String search, Long categoryId, Long brandId, String status, List<String> selectedColumns, String format) throws Exception {
        List<ProductCatalogExportDTO> rawData = reportRepository.getProductCatalogExport(search, categoryId, brandId, status);
        List<ProductCatalogExportItem> data = new ArrayList<>();
        if (rawData != null) {
            for (ProductCatalogExportDTO dto : rawData) {
                if (dto != null) {
                    data.add(new ProductCatalogExportItem(
                            dto.getProductName(),
                            dto.getSku(),
                            dto.getPrice(),
                            dto.getStock(),
                            dto.getStatus()
                    ));
                }
            }
        }
        JasperDesign jasperDesign = buildProductCatalogJasperDesign(selectedColumns);
        JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), new JRBeanCollectionDataSource(data));
        return exportReport(jasperPrint, format);
    }

    private JasperDesign buildProductCatalogJasperDesign(List<String> selectedColumns) throws Exception {
        // 1. Resolve and filter requested columns against available columns
        List<ColumnDef> activeColumns = new ArrayList<>();
        if (selectedColumns != null && !selectedColumns.isEmpty()) {
            for (String col : selectedColumns) {
                if (col == null) continue;
                String trimmed = col.trim();
                for (Map.Entry<String, ColumnDef> entry : AVAILABLE_COLUMNS.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(trimmed) || entry.getKey().equalsIgnoreCase(trimmed.replace("_", ""))) {
                        if (!activeColumns.contains(entry.getValue())) {
                            activeColumns.add(entry.getValue());
                        }
                        break;
                    }
                }
            }
        }
        if (activeColumns.isEmpty()) {
            activeColumns.addAll(AVAILABLE_COLUMNS.values());
        }

        // 2. Initialize JasperDesign with standard A4 Portrait dimensions
        JasperDesign jasperDesign = new JasperDesign();
        jasperDesign.setName("ProductCatalogExportReport");
        jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        jasperDesign.setPageWidth(595);
        jasperDesign.setPageHeight(842);
        jasperDesign.setColumnWidth(555);
        jasperDesign.setLeftMargin(20);
        jasperDesign.setRightMargin(20);
        jasperDesign.setTopMargin(20);
        jasperDesign.setBottomMargin(20);

        // 3. Register all available fields on the report so bean mappings succeed
        for (ColumnDef def : AVAILABLE_COLUMNS.values()) {
            JRDesignField field = new JRDesignField();
            field.setName(def.key);
            field.setValueClass(def.valueClass);
            jasperDesign.addField(field);
        }

        // 4. Corporate Title Band (height = 55)
        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(55);

        JRDesignStaticText systemText = new JRDesignStaticText();
        systemText.setX(0);
        systemText.setY(0);
        systemText.setWidth(350);
        systemText.setHeight(20);
        systemText.setText("E-COMMERCE BACK-OFFICE SYSTEM");
        systemText.setFontName("SansSerif");
        systemText.setFontSize(14f);
        systemText.setBold(true);
        systemText.setForecolor(Color.decode("#1E293B"));
        systemText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        titleBand.addElement(systemText);

        JRDesignStaticText reportTitleText = new JRDesignStaticText();
        reportTitleText.setX(0);
        reportTitleText.setY(22);
        reportTitleText.setWidth(350);
        reportTitleText.setHeight(18);
        reportTitleText.setText("PRODUCT LIST REPORT");
        reportTitleText.setFontName("SansSerif");
        reportTitleText.setFontSize(12f);
        reportTitleText.setBold(true);
        reportTitleText.setForecolor(Color.decode("#475569"));
        reportTitleText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        titleBand.addElement(reportTitleText);

        JRDesignTextField dateField = new JRDesignTextField();
        dateField.setX(355);
        dateField.setY(10);
        dateField.setWidth(200);
        dateField.setHeight(20);
        dateField.setFontName("SansSerif");
        dateField.setFontSize(9f);
        dateField.setForecolor(Color.decode("#64748B"));
        dateField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        dateField.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        JRDesignExpression dateExpr = new JRDesignExpression();
        dateExpr.setText("\"Generated on: \" + new java.text.SimpleDateFormat(\"yyyy-MM-dd HH:mm\").format(new java.util.Date())");
        dateField.setExpression(dateExpr);
        titleBand.addElement(dateField);

        JRDesignLine headerLine = new JRDesignLine();
        headerLine.setX(0);
        headerLine.setY(48);
        headerLine.setWidth(555);
        headerLine.setHeight(1);
        headerLine.setForecolor(Color.decode("#CBD5E1"));
        titleBand.addElement(headerLine);

        jasperDesign.setTitle(titleBand);

        // 5. Column Header Band (height = 28) & Detail Band (height = 24)
        JRDesignBand columnHeaderBand = new JRDesignBand();
        columnHeaderBand.setHeight(28);

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(24);

        // First Column: "No." (width = 35px)
        int noWidth = 35;

        JRDesignStaticText noHeader = new JRDesignStaticText();
        noHeader.setX(0);
        noHeader.setY(0);
        noHeader.setWidth(noWidth);
        noHeader.setHeight(28);
        noHeader.setText("No.");
        noHeader.setMode(ModeEnum.OPAQUE);
        noHeader.setBackcolor(Color.decode("#1E293B"));
        noHeader.setForecolor(Color.WHITE);
        noHeader.setFontName("SansSerif");
        noHeader.setFontSize(10f);
        noHeader.setBold(true);
        noHeader.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        noHeader.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        noHeader.getLineBox().getPen().setLineWidth(0.5f);
        noHeader.getLineBox().getPen().setLineColor(Color.decode("#CBD5E1"));
        columnHeaderBand.addElement(noHeader);

        JRDesignTextField noDetail = new JRDesignTextField();
        noDetail.setX(0);
        noDetail.setY(0);
        noDetail.setWidth(noWidth);
        noDetail.setHeight(24);
        noDetail.setFontName("SansSerif");
        noDetail.setFontSize(9f);
        noDetail.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        noDetail.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        noDetail.setBlankWhenNull(true);
        noDetail.getLineBox().getPen().setLineWidth(0.5f);
        noDetail.getLineBox().getPen().setLineColor(Color.decode("#CBD5E1"));
        noDetail.getLineBox().setLeftPadding(5);
        noDetail.getLineBox().setRightPadding(5);
        JRDesignExpression noExpr = new JRDesignExpression();
        noExpr.setText("$V{REPORT_COUNT}");
        noDetail.setExpression(noExpr);
        detailBand.addElement(noDetail);

        // Dynamic Columns (evenly distribute remaining 520px)
        int remainingWidth = 555 - noWidth; // 520px
        int numCols = activeColumns.size();
        int baseWidth = remainingWidth / numCols;
        int remainder = remainingWidth % numCols;

        int currentX = noWidth;
        for (int i = 0; i < numCols; i++) {
            ColumnDef colDef = activeColumns.get(i);
            int colWidth = baseWidth + (i == numCols - 1 ? remainder : 0);

            // Column Header
            JRDesignStaticText colHeader = new JRDesignStaticText();
            colHeader.setX(currentX);
            colHeader.setY(0);
            colHeader.setWidth(colWidth);
            colHeader.setHeight(28);
            colHeader.setText(colDef.header);
            colHeader.setMode(ModeEnum.OPAQUE);
            colHeader.setBackcolor(Color.decode("#1E293B"));
            colHeader.setForecolor(Color.WHITE);
            colHeader.setFontName("SansSerif");
            colHeader.setFontSize(10f);
            colHeader.setBold(true);
            colHeader.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            colHeader.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            colHeader.getLineBox().getPen().setLineWidth(0.5f);
            colHeader.getLineBox().getPen().setLineColor(Color.decode("#CBD5E1"));
            columnHeaderBand.addElement(colHeader);

            // Detail Cell
            JRDesignTextField colDetail = new JRDesignTextField();
            colDetail.setX(currentX);
            colDetail.setY(0);
            colDetail.setWidth(colWidth);
            colDetail.setHeight(24);
            colDetail.setFontName("SansSerif");
            colDetail.setFontSize(9f);
            colDetail.setHorizontalTextAlign(colDef.detailAlign);
            colDetail.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            if (colDef.pattern != null) {
                colDetail.setPattern(colDef.pattern);
            }
            colDetail.setBlankWhenNull(true);
            colDetail.getLineBox().getPen().setLineWidth(0.5f);
            colDetail.getLineBox().getPen().setLineColor(Color.decode("#CBD5E1"));
            colDetail.getLineBox().setLeftPadding(5);
            colDetail.getLineBox().setRightPadding(5);
            JRDesignExpression colExpr = new JRDesignExpression();
            colExpr.setText("$F{" + colDef.key + "}");
            colDetail.setExpression(colExpr);
            detailBand.addElement(colDetail);

            currentX += colWidth;
        }

        jasperDesign.setColumnHeader(columnHeaderBand);
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(detailBand);

        // 6. Summary Band (displays a message row if no records match)
        JRDesignBand summaryBand = new JRDesignBand();
        summaryBand.setHeight(26);

        JRDesignStaticText noDataText = new JRDesignStaticText();
        noDataText.setX(0);
        noDataText.setY(0);
        noDataText.setWidth(555);
        noDataText.setHeight(26);
        noDataText.setText("No product records found matching the selected filter criteria.");
        noDataText.setFontName("SansSerif");
        noDataText.setFontSize(9f);
        noDataText.setItalic(true);
        noDataText.setMode(ModeEnum.OPAQUE);
        noDataText.setBackcolor(Color.decode("#F8FAFC"));
        noDataText.setForecolor(Color.decode("#64748B"));
        noDataText.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        noDataText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        noDataText.getLineBox().getPen().setLineWidth(0.5f);
        noDataText.getLineBox().getPen().setLineColor(Color.decode("#CBD5E1"));
        summaryBand.addElement(noDataText);

        JRDesignExpression noDataBandExpr = new JRDesignExpression();
        noDataBandExpr.setText("$V{REPORT_COUNT} == null || $V{REPORT_COUNT} == 0");
        summaryBand.setPrintWhenExpression(noDataBandExpr);

        jasperDesign.setSummary(summaryBand);

        // 6. Page Footer Band (height = 25)
        JRDesignBand footerBand = new JRDesignBand();
        footerBand.setHeight(25);

        JRDesignStaticText confText = new JRDesignStaticText();
        confText.setX(0);
        confText.setY(5);
        confText.setWidth(300);
        confText.setHeight(15);
        confText.setText("Confidential - Internal Back-Office Use Only");
        confText.setFontName("SansSerif");
        confText.setFontSize(8f);
        confText.setItalic(true);
        confText.setForecolor(Color.decode("#64748B"));
        confText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        footerBand.addElement(confText);

        JRDesignTextField pageField = new JRDesignTextField();
        pageField.setX(400);
        pageField.setY(5);
        pageField.setWidth(155);
        pageField.setHeight(15);
        pageField.setFontName("SansSerif");
        pageField.setFontSize(8f);
        pageField.setForecolor(Color.decode("#64748B"));
        pageField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        pageField.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        JRDesignExpression pageExpr = new JRDesignExpression();
        pageExpr.setText("\"Page \" + $V{PAGE_NUMBER}");
        pageField.setExpression(pageExpr);
        footerBand.addElement(pageField);

        jasperDesign.setPageFooter(footerBand);

        return jasperDesign;
    }

    // =========================================================================
    // DYNAMIC COLUMN BRAND MANAGEMENT EXPORT (PDF & EXCEL)
    // =========================================================================

    public static class BrandExportItem {
        private String brandName;
        private String categoryName;
        private String description;
        private String status;

        public BrandExportItem(String brandName, String categoryName, String description, String status) {
            this.brandName = brandName != null && !brandName.isBlank() ? brandName : "-";
            this.categoryName = categoryName != null && !categoryName.isBlank() ? categoryName : "No category";
            this.description = description != null && !description.isBlank() ? description : "-";
            this.status = status != null && !status.isBlank() ? status : "UNKNOWN";
        }

        public String getBrandName() { return brandName; }
        public String getCategoryName() { return categoryName; }
        public String getDescription() { return description; }
        public String getStatus() { return status; }
    }

    private static final Map<String, ColumnDef> BRAND_AVAILABLE_COLUMNS = new LinkedHashMap<>();
    static {
        BRAND_AVAILABLE_COLUMNS.put("brandName", new ColumnDef("brandName", "BRAND", String.class, HorizontalTextAlignEnum.LEFT, null));
        BRAND_AVAILABLE_COLUMNS.put("categoryName", new ColumnDef("categoryName", "CATEGORY", String.class, HorizontalTextAlignEnum.LEFT, null));
        BRAND_AVAILABLE_COLUMNS.put("description", new ColumnDef("description", "DESCRIPTION", String.class, HorizontalTextAlignEnum.LEFT, null));
        BRAND_AVAILABLE_COLUMNS.put("status", new ColumnDef("status", "STATUS", String.class, HorizontalTextAlignEnum.CENTER, null));
    }

    public byte[] generateDynamicBrandPdf(String search, String status, List<String> selectedColumns) throws Exception {
        return generateDynamicBrandReport(search, status, selectedColumns, "pdf");
    }

    public byte[] generateDynamicBrandExcel(String search, String status, List<String> selectedColumns) throws Exception {
        return generateDynamicBrandReport(search, status, selectedColumns, "excel");
    }

    public byte[] generateDynamicBrandReport(String search, String status, List<String> selectedColumns, String format) throws Exception {
        List<BrandExportDTO> rawData = reportRepository.getBrandExportReport(search, status);
        List<BrandExportItem> data = new ArrayList<>();
        if (rawData != null) {
            for (BrandExportDTO dto : rawData) {
                if (dto != null) {
                    data.add(new BrandExportItem(
                            dto.getBrandName(),
                            dto.getCategoryName(),
                            dto.getDescription(),
                            dto.getStatus()
                    ));
                }
            }
        }
        JasperDesign jasperDesign = buildBrandJasperDesign(selectedColumns, status);
        JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), new JRBeanCollectionDataSource(data));
        return exportReport(jasperPrint, format);
    }

    private JasperDesign buildBrandJasperDesign(List<String> selectedColumns, String statusFilter) throws Exception {
        // 1. Resolve and filter requested columns against available columns
        List<ColumnDef> activeColumns = new ArrayList<>();
        if (selectedColumns != null && !selectedColumns.isEmpty()) {
            for (String col : selectedColumns) {
                if (col == null) continue;
                String trimmed = col.trim();
                for (Map.Entry<String, ColumnDef> entry : BRAND_AVAILABLE_COLUMNS.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(trimmed) || entry.getKey().equalsIgnoreCase(trimmed.replace("_", ""))) {
                        if (!activeColumns.contains(entry.getValue())) {
                            activeColumns.add(entry.getValue());
                        }
                        break;
                    }
                }
            }
        }
        if (activeColumns.isEmpty()) {
            activeColumns.addAll(BRAND_AVAILABLE_COLUMNS.values());
        }

        // 2. Initialize JasperDesign with standard A4 Portrait dimensions
        JasperDesign jasperDesign = new JasperDesign();
        jasperDesign.setName("BrandManagementExportReport");
        jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        jasperDesign.setPageWidth(595);
        jasperDesign.setPageHeight(842);
        jasperDesign.setColumnWidth(555);
        jasperDesign.setLeftMargin(20);
        jasperDesign.setRightMargin(20);
        jasperDesign.setTopMargin(20);
        jasperDesign.setBottomMargin(20);

        // 3. Register all available fields on the report
        for (ColumnDef def : BRAND_AVAILABLE_COLUMNS.values()) {
            JRDesignField field = new JRDesignField();
            field.setName(def.key);
            field.setValueClass(def.valueClass);
            jasperDesign.addField(field);
        }

        // 4. Corporate Title Band (height = 65)
        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(65);

        JRDesignStaticText systemText = new JRDesignStaticText();
        systemText.setX(0);
        systemText.setY(0);
        systemText.setWidth(350);
        systemText.setHeight(20);
        systemText.setText("E-COMMERCE BACK-OFFICE SYSTEM");
        systemText.setFontName("SansSerif");
        systemText.setFontSize(14f);
        systemText.setBold(true);
        systemText.setForecolor(Color.decode("#1E293B"));
        systemText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        titleBand.addElement(systemText);

        JRDesignStaticText reportTitleText = new JRDesignStaticText();
        reportTitleText.setX(0);
        reportTitleText.setY(22);
        reportTitleText.setWidth(350);
        reportTitleText.setHeight(18);
        reportTitleText.setText("BRAND MANAGEMENT REPORT");
        reportTitleText.setFontName("SansSerif");
        reportTitleText.setFontSize(12f);
        reportTitleText.setBold(true);
        reportTitleText.setForecolor(Color.decode("#475569"));
        reportTitleText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        titleBand.addElement(reportTitleText);

        String activeStatus = (statusFilter != null && !statusFilter.isBlank()) ? statusFilter.toUpperCase() : "ALL";
        JRDesignStaticText filterSubtitle = new JRDesignStaticText();
        filterSubtitle.setX(0);
        filterSubtitle.setY(42);
        filterSubtitle.setWidth(350);
        filterSubtitle.setHeight(14);
        filterSubtitle.setText("Filter Status: " + activeStatus);
        filterSubtitle.setFontName("SansSerif");
        filterSubtitle.setFontSize(9f);
        filterSubtitle.setBold(true);
        filterSubtitle.setForecolor(Color.decode("#64748B"));
        filterSubtitle.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        titleBand.addElement(filterSubtitle);

        JRDesignTextField dateField = new JRDesignTextField();
        dateField.setX(355);
        dateField.setY(10);
        dateField.setWidth(200);
        dateField.setHeight(20);
        dateField.setFontName("SansSerif");
        dateField.setFontSize(9f);
        dateField.setForecolor(Color.decode("#64748B"));
        dateField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        dateField.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        JRDesignExpression dateExpr = new JRDesignExpression();
        dateExpr.setText("\"Generated on: \" + new java.text.SimpleDateFormat(\"yyyy-MM-dd HH:mm\").format(new java.util.Date())");
        dateField.setExpression(dateExpr);
        titleBand.addElement(dateField);

        JRDesignLine headerLine = new JRDesignLine();
        headerLine.setX(0);
        headerLine.setY(60);
        headerLine.setWidth(555);
        headerLine.setHeight(1);
        headerLine.setForecolor(Color.decode("#CBD5E1"));
        titleBand.addElement(headerLine);

        jasperDesign.setTitle(titleBand);

        // 5. Column Header Band (height = 28) & Detail Band (height = 24)
        JRDesignBand columnHeaderBand = new JRDesignBand();
        columnHeaderBand.setHeight(28);

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(24);

        // First Column: "#" / "No." (width = 35px)
        int noWidth = 35;

        JRDesignStaticText noHeader = new JRDesignStaticText();
        noHeader.setX(0);
        noHeader.setY(0);
        noHeader.setWidth(noWidth);
        noHeader.setHeight(28);
        noHeader.setText("No.");
        noHeader.setMode(ModeEnum.OPAQUE);
        noHeader.setBackcolor(Color.decode("#1E293B"));
        noHeader.setForecolor(Color.WHITE);
        noHeader.setFontName("SansSerif");
        noHeader.setFontSize(10f);
        noHeader.setBold(true);
        noHeader.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        noHeader.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        noHeader.getLineBox().getPen().setLineWidth(0.5f);
        noHeader.getLineBox().getPen().setLineColor(Color.decode("#CBD5E1"));
        columnHeaderBand.addElement(noHeader);

        JRDesignTextField noDetail = new JRDesignTextField();
        noDetail.setX(0);
        noDetail.setY(0);
        noDetail.setWidth(noWidth);
        noDetail.setHeight(24);
        noDetail.setFontName("SansSerif");
        noDetail.setFontSize(9f);
        noDetail.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        noDetail.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        noDetail.setBlankWhenNull(true);
        noDetail.getLineBox().getPen().setLineWidth(0.5f);
        noDetail.getLineBox().getPen().setLineColor(Color.decode("#CBD5E1"));
        noDetail.getLineBox().setLeftPadding(5);
        noDetail.getLineBox().setRightPadding(5);
        JRDesignExpression noExpr = new JRDesignExpression();
        noExpr.setText("$V{REPORT_COUNT}");
        noDetail.setExpression(noExpr);
        detailBand.addElement(noDetail);

        // Dynamic Columns (evenly distribute remaining 520px)
        int remainingWidth = 555 - noWidth; // 520px
        int numCols = activeColumns.size();
        int baseWidth = remainingWidth / numCols;
        int remainder = remainingWidth % numCols;

        int currentX = noWidth;
        for (int i = 0; i < numCols; i++) {
            ColumnDef colDef = activeColumns.get(i);
            int colWidth = baseWidth + (i == numCols - 1 ? remainder : 0);

            // Column Header
            JRDesignStaticText colHeader = new JRDesignStaticText();
            colHeader.setX(currentX);
            colHeader.setY(0);
            colHeader.setWidth(colWidth);
            colHeader.setHeight(28);
            colHeader.setText(colDef.header);
            colHeader.setMode(ModeEnum.OPAQUE);
            colHeader.setBackcolor(Color.decode("#1E293B"));
            colHeader.setForecolor(Color.WHITE);
            colHeader.setFontName("SansSerif");
            colHeader.setFontSize(10f);
            colHeader.setBold(true);
            colHeader.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            colHeader.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            colHeader.getLineBox().getPen().setLineWidth(0.5f);
            colHeader.getLineBox().getPen().setLineColor(Color.decode("#CBD5E1"));
            columnHeaderBand.addElement(colHeader);

            // Detail Cell
            JRDesignTextField colDetail = new JRDesignTextField();
            colDetail.setX(currentX);
            colDetail.setY(0);
            colDetail.setWidth(colWidth);
            colDetail.setHeight(24);
            colDetail.setFontName("SansSerif");
            colDetail.setFontSize(9f);
            colDetail.setHorizontalTextAlign(colDef.detailAlign);
            colDetail.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            colDetail.setBlankWhenNull(true);
            colDetail.getLineBox().getPen().setLineWidth(0.5f);
            colDetail.getLineBox().getPen().setLineColor(Color.decode("#CBD5E1"));
            colDetail.getLineBox().setLeftPadding(5);
            colDetail.getLineBox().setRightPadding(5);
            JRDesignExpression colExpr = new JRDesignExpression();
            colExpr.setText("$F{" + colDef.key + "}");
            colDetail.setExpression(colExpr);
            detailBand.addElement(colDetail);

            currentX += colWidth;
        }

        jasperDesign.setColumnHeader(columnHeaderBand);
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(detailBand);

        // 6. Summary Band (displays a message row if no records match)
        JRDesignBand summaryBand = new JRDesignBand();
        summaryBand.setHeight(26);

        JRDesignStaticText noDataText = new JRDesignStaticText();
        noDataText.setX(0);
        noDataText.setY(0);
        noDataText.setWidth(555);
        noDataText.setHeight(26);
        noDataText.setText("No brand records found matching the selected filter criteria.");
        noDataText.setFontName("SansSerif");
        noDataText.setFontSize(9f);
        noDataText.setItalic(true);
        noDataText.setMode(ModeEnum.OPAQUE);
        noDataText.setBackcolor(Color.decode("#F8FAFC"));
        noDataText.setForecolor(Color.decode("#64748B"));
        noDataText.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        noDataText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        noDataText.getLineBox().getPen().setLineWidth(0.5f);
        noDataText.getLineBox().getPen().setLineColor(Color.decode("#CBD5E1"));
        summaryBand.addElement(noDataText);

        JRDesignExpression noDataBandExpr = new JRDesignExpression();
        noDataBandExpr.setText("$V{REPORT_COUNT} == null || $V{REPORT_COUNT} == 0");
        summaryBand.setPrintWhenExpression(noDataBandExpr);

        jasperDesign.setSummary(summaryBand);

        // 7. Page Footer Band (height = 25)
        JRDesignBand footerBand = new JRDesignBand();
        footerBand.setHeight(25);

        JRDesignStaticText confText = new JRDesignStaticText();
        confText.setX(0);
        confText.setY(5);
        confText.setWidth(300);
        confText.setHeight(15);
        confText.setText("Confidential - Internal Back-Office Use Only");
        confText.setFontName("SansSerif");
        confText.setFontSize(8f);
        confText.setItalic(true);
        confText.setForecolor(Color.decode("#64748B"));
        confText.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        footerBand.addElement(confText);

        JRDesignTextField pageField = new JRDesignTextField();
        pageField.setX(400);
        pageField.setY(5);
        pageField.setWidth(155);
        pageField.setHeight(15);
        pageField.setFontName("SansSerif");
        pageField.setFontSize(8f);
        pageField.setForecolor(Color.decode("#64748B"));
        pageField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        pageField.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        JRDesignExpression pageExpr = new JRDesignExpression();
        pageExpr.setText("\"Page \" + $V{PAGE_NUMBER}");
        pageField.setExpression(pageExpr);
        footerBand.addElement(pageField);

        jasperDesign.setPageFooter(footerBand);

        return jasperDesign;
    }
}
