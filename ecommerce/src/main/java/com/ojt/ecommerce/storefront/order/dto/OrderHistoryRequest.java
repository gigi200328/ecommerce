package com.ojt.ecommerce.storefront.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Query parameters for the paginated order history endpoint.
 * Binds to GET /api/v1/customers/{customerId}/orders
 */
@Data
public class OrderHistoryRequest {

    /**
     * Maximum rows per page (1-100, default 20).
     */
    @Min(value = 1, message = "limit must be at least 1")
    @Max(value = 100, message = "limit must not exceed 100")
    private Integer limit = 20;

    /**
     * Zero-based row offset for pagination (default 0).
     */
    @Min(value = 0, message = "offset must be 0 or greater")
    private Integer offset = 0;

    /**
     * Optional order status filter.
     * Allowed values mirror the DB ENUM: PENDING, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED.
     */
    @Pattern(
        regexp = "^(PENDING|PAID|PROCESSING|SHIPPED|DELIVERED|CANCELLED)$",
        message = "orderStatus must be one of: PENDING, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED"
    )
    private String orderStatus;
}
