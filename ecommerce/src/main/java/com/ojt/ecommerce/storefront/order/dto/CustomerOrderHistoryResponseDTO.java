package com.ojt.ecommerce.storefront.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Complete paginated response for the customer order history endpoint.
 * Wraps a list of {@link OrderSummaryDTO} records along with
 * server-computed pagination metadata.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerOrderHistoryResponseDTO {

    // ── Pagination Metadata ────────────────────────────────────────────────
    /** Total orders that match the filter (computed by MySQL window function). */
    private long totalItems;

    /** Total number of pages given the requested limit. */
    private int totalPages;

    /** Current page number (0-based, derived from offset / limit). */
    private int currentPage;

    /** Number of items requested per page. */
    private int pageSize;

    /** Whether this is the first page. */
    private boolean first;

    /** Whether this is the last page. */
    private boolean last;

    // ── Payload ───────────────────────────────────────────────────────────
    private List<OrderSummaryDTO> orders;

    // ── Nested Summary DTO ────────────────────────────────────────────────

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderSummaryDTO {
        private Long orderId;
        private String orderNo;
        private String orderStatus;
        private String paymentStatus;
        private LocalDateTime createdAt;

        // Financial breakdown
        private BigDecimal subtotalAmount;
        private BigDecimal shippingFee;
        private BigDecimal taxAmount;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;

        // Summary metrics returned by the SP
        private int totalLineItems;
        private int totalItemQuantity;
        private String latestPaymentMethod;
    }
}
