package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.dto.MonthlyProfitComparisonDto;

public interface ProfitReportService {

    MonthlyProfitComparisonDto getMonthOnMonthProfitReport(Integer year);

    byte[] exportMonthOnMonthProfitExcel(Integer year);
}
