package com.ojt.ecommerce.storefront.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregated invoice / receipt response for a single order.
 * Contains two logical sections that mirror the two result sets
 * returned by {@code sp_get_order_invoice_detail}:
 * <ol>
 *   <li>Order header, customer, shipping address, payment, and shipment info.</li>
 *   <li>Order line items.</li>
 * </ol>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderInvoiceResponseDTO {

    // ── Result Set 1: Order Header ─────────────────────────────────────────
    private Long orderId;
    private String orderNo;
    private String orderStatus;
    private String paymentStatus;
    private LocalDateTime orderPlacedAt;
    private LocalDateTime orderModifiedAt;

    // Financial totals
    private BigDecimal subtotalAmount;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    // ── Customer Info ─────────────────────────────────────────────────────
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // ── Shipping / Delivery Address ───────────────────────────────────────
    private ShippingAddressDTO shippingAddress;

    // ── Latest Payment Info ───────────────────────────────────────────────
    private PaymentInfoDTO paymentInfo;

    // ── Active Shipment Tracking ──────────────────────────────────────────
    private ShipmentTrackingDTO shipmentTracking;

    // ── Result Set 2: Line Items ──────────────────────────────────────────
    private List<OrderItemDetailDTO> items;

    // ═════════════════════════════════════════════════════════════════════
    // Nested DTOs
    // ═════════════════════════════════════════════════════════════════════

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShippingAddressDTO {
        private String recipientName;
        private String recipientPhone;
        private String addressLine1;
        private String addressLine2;
        private String township;
        private String city;
        private String regionOrState;
        /** Pre-formatted full address text built by MySQL CONCAT_WS. */
        private String fullAddressText;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentInfoDTO {
        private Long paymentId;
        private String paymentMethod;
        private String paymentStatus;
        private String transactionRef;
        private LocalDateTime paidAt;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShipmentTrackingDTO {
        private Long shipmentId;
        private String courierName;
        private String trackingNumber;
        private String shipmentStatus;
        private LocalDateTime shippedAt;
        private LocalDateTime deliveredAt;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderItemDetailDTO {
        private Long orderItemId;
        private String productName;
        private String sku;
        /**
         * Raw JSON string from the {@code variant_attributes} column.
         * Kept as String so callers can deserialize to their preferred structure.
         */
        private String variantAttributes;
        private BigDecimal unitPrice;
        private Integer qty;
        private BigDecimal discountAmount;
        private BigDecimal subtotal;
    }
}
