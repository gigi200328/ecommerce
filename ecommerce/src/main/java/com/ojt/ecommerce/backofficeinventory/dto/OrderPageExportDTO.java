package com.ojt.ecommerce.backofficeinventory.dto;

import java.math.BigDecimal;

/**
 * Interface-based DTO Projection for mapping SP_GetOrdersPageExportReport results.
 * Spring Data JPA automatically maps the result set columns:
 * - order_no / orderNo             -> getOrderNo()
 * - order_date / orderDate         -> getOrderDate()
 * - customer_name / customerName   -> getCustomerName()
 * - total_amount / totalAmount     -> getTotalAmount()
 * - order_status / orderStatus     -> getOrderStatus()
 * - payment_status / paymentStatus -> getPaymentStatus()
 */
public interface OrderPageExportDTO {

    String getOrderNo();

    String getOrderDate();

    String getCustomerName();

    BigDecimal getTotalAmount();

    String getOrderStatus();

    String getPaymentStatus();
}
