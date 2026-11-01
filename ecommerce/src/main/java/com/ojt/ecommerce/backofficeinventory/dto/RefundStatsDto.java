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
public class RefundStatsDto {

    private long totalRequests;

    private long pendingCount;

    private long approvedCount;

    private long rejectedCount;

    private BigDecimal totalRefundedAmount;
}
