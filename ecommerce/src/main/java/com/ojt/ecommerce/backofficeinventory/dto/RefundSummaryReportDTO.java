package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Interface-based DTO Projection for mapping the Refund Summary native query results.
 */
public interface RefundSummaryReportDTO {
    
    Long getRefundId();             // Maps to refund_id
    
    Long getOrderId();              // Maps to order_id

    String getOrderNo();            // Maps to order_no

    String getCustomerName();       // Maps to customer_name
    
    String getProductName();        // Maps to product_name
    
    Integer getRefundQuantity();    // Maps to refund_quantity
    
    BigDecimal getRefundAmount();   // Maps to refund_amount
    
    String getRequestReason();      // Maps to request_reason

    String getReason();             // Maps to reason
    
    String getStatus();             // Maps to status
    
    Object getRequestedDate();      // Maps to requested_date
}
