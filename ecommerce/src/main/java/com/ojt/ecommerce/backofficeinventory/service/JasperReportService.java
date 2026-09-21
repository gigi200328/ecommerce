package com.ojt.ecommerce.backofficeinventory.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ojt.ecommerce.backofficeinventory.dto.DailyProductSalesDTO;
import com.ojt.ecommerce.backofficeinventory.repository.ReportRepository;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
@RequiredArgsConstructor
public class JasperReportService {

    private final ReportRepository reportRepository;

    public byte[] generateDailyProductSalesPdf(LocalDate startDate, LocalDate endDate) throws Exception {
        // 1. Fetch data from the database using the Stored Procedure
        List<DailyProductSalesDTO> salesData = reportRepository.getDailyProductSales(startDate, endDate);

        // 2. Load and compile the Jasper Report (.jrxml) template
        // The file should be placed in src/main/resources/reports/DailyProductSalesReport.jrxml
        InputStream reportStream = getClass().getResourceAsStream("/reports/DailyProductSalesReport.jrxml");
        if (reportStream == null) {
            throw new RuntimeException("Report template not found in /reports/DailyProductSalesReport.jrxml");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // 3. Map the data to a Jasper Reports Data Source
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(salesData);

        // 4. Set up report parameters (if any, e.g., report generation date or filters)
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Daily E-commerce Product Sales Report");
        parameters.put("START_DATE", startDate.toString());
        parameters.put("END_DATE", endDate.toString());

        // 5. Fill the report with data and parameters
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        // 6. Export the report to a PDF byte array
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
}
