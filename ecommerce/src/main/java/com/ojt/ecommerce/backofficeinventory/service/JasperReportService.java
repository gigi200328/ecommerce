package com.ojt.ecommerce.backofficeinventory.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ojt.ecommerce.backofficeinventory.dto.LowStockAlertDTO;
import com.ojt.ecommerce.backofficeinventory.dto.RefundSummaryReportDTO;
import com.ojt.ecommerce.backofficeinventory.dto.TopSellingProductDTO;
import com.ojt.ecommerce.backofficeinventory.dto.CategorySummaryDTO;
import com.ojt.ecommerce.backofficeinventory.dto.DailyProductSalesDTO;
import com.ojt.ecommerce.backofficeinventory.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
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
            // Optional configuration can be added here
            // SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            // exporter.setConfiguration(configuration);
            exporter.exportReport();
            return outputStream.toByteArray();
        } else {
            // Default to PDF
            return JasperExportManager.exportReportToPdf(jasperPrint);
        }
    }

    public byte[] generateDailyProductSalesReport(LocalDate startDate, LocalDate endDate, String format) throws Exception {
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
}
