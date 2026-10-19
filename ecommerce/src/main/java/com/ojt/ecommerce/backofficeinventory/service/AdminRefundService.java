package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDate;
import java.util.List;

import com.ojt.ecommerce.backofficeinventory.dto.AdminRefundResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.RefundDecisionDto;
import com.ojt.ecommerce.backofficeinventory.dto.RefundStatsDto;

public interface AdminRefundService {

    List<AdminRefundResponseDto> getRefunds(String search, String status, LocalDate startDate, LocalDate endDate);

    RefundStatsDto getRefundStats();

    AdminRefundResponseDto approveRefund(Long refundId, RefundDecisionDto request);

    AdminRefundResponseDto rejectRefund(Long refundId, RefundDecisionDto request);
}
