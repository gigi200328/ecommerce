package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Interface-based DTO Projection for mapping the Order Fulfillment Status native query results.
 * Spring Data JPA automatically maps the snake_case columns from SP_GetOrderFulfillmentReport:
 * - order_id       -> getOrderId()
 * - order_date     -> getOrderDate()
 * - total_items    -> getTotalItems()
 * - total_amount   -> getTotalAmount()
 * - payment_status -> getPaymentStatus()
 * - order_status   -> getOrderStatus()
 */
public interface OrderFulfillmentReportDTO {

    Long getOrderId();

    Date getOrderDate();

    Long getTotalItems();

    BigDecimal getTotalAmount();

    String getPaymentStatus();

    String getOrderStatus();
}
