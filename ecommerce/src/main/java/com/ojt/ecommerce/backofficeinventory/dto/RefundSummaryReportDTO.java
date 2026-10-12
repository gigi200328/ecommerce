package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Interface-based DTO Projection for mapping the Refund Summary native query results.
 */
public interface RefundSummaryReportDTO {
    
    Long getRefundId();             // Maps to refund_id
    
    Long getOrderId();              // Maps to order_id
    
    String getProductName();        // Maps to product_name
    
    Integer getRefundQuantity();    // Maps to refund_quantity
    
    BigDecimal getRefundAmount();   // Maps to refund_amount
    
    String getRequestReason();      // Maps to request_reason
    
    String getStatus();             // Maps to status
    
    Date getRequestedDate();        // Maps to requested_date
}
