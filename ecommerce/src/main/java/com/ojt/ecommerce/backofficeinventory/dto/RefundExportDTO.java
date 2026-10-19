package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;

/**
 * Interface-based DTO Projection for mapping SP_GetRefundSummaryReport results.
 * Spring Data JPA automatically maps the result set columns:
 * - refund_id / refundId         -> getRefundId()
 * - order_no / orderNo           -> getOrderNo()
 * - customer_name / customerName -> getCustomerName()
 * - refund_amount / refundAmount -> getRefundAmount()
 * - reason / requestReason       -> getReason()
 * - status                       -> getStatus()
 * - requested_date / requestedDate -> getRequestedDate()
 */
public interface RefundExportDTO {

    Long getRefundId();

    String getOrderNo();

    String getCustomerName();

    BigDecimal getRefundAmount();

    String getReason();

    String getStatus();

    String getRequestedDate();
}
