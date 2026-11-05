package com.ojt.ecommerce.storefront.order.repository;

import com.ojt.ecommerce.storefront.order.dto.CustomerOrderHistoryResponseDTO.OrderSummaryDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderHistoryReportDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderInvoiceResponseDTO.OrderItemDetailDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repository that executes the two read-only stored procedures via JDBC.
 *
 * <h3>Why JdbcTemplate / raw JDBC instead of {@code @Procedure}?</h3>
 * <p>Spring Data JPA's {@code @Procedure} maps only a <em>single</em> result set
 * that must be backed by a JPA {@code @Entity}.  Both procedures here return
 * custom projections with no entity mapping, and
 * {@code sp_get_order_invoice_detail} returns <em>two</em> result sets in one
 * call.  {@link JdbcTemplate} (for SP 1) and a raw {@link DataSource} connection
 * (for SP 2) are the correct, production-safe tools.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderInspectionRepository {

    /** Used for the single-result-set history procedure. */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Used for the two-result-set invoice procedure.
     * Raw DataSource access is required here because {@link JdbcTemplate} does
     * not expose a public API for advancing through multiple result sets via
     * {@link Statement#getMoreResults()}.
     */
    private final DataSource dataSource;

    // =========================================================================
    // Result-holder records  (Java 16+)
    // =========================================================================

    /**
     * Pairs the mapped page rows with the server-computed total-matching-orders
     * count that comes from the MySQL {@code COUNT(*) OVER()} window function.
     */
    public record OrderHistoryResult(
            List<OrderSummaryDTO> orders,
            long totalMatchingOrders
    ) {}

    /**
     * Holds both result sets from {@code sp_get_order_invoice_detail}.
     */
    public record InvoiceRawResult(
            Optional<InvoiceHeaderRow> header,
            List<OrderItemDetailDTO> items
    ) {}

    /**
     * Flat projection of every column returned in Result Set 1 of the invoice
     * procedure.  The service layer assembles the nested response DTOs from
     * these fields.
     */
    public record InvoiceHeaderRow(
            // ── Order header ──────────────────────────────────────────────
            Long orderId, String orderNo, String orderStatus, String paymentStatus,
            LocalDateTime orderPlacedAt, LocalDateTime orderModifiedAt,
            BigDecimal subtotalAmount, BigDecimal shippingFee,
            BigDecimal taxAmount, BigDecimal discountAmount, BigDecimal totalAmount,
            // ── Customer ──────────────────────────────────────────────────
            Long customerId, String customerName, String customerEmail, String customerPhone,
            // ── Shipping address ──────────────────────────────────────────
            String recipientName, String recipientPhone,
            String addressLine1, String addressLine2,
            String township, String city, String regionOrState, String fullAddressText,
            // ── Latest payment (nullable — order may have no payment yet) ─
            Long paymentId, String paymentMethod, String latestPaymentStatus,
            String transactionRef, LocalDateTime paidAt, BigDecimal paymentAmount,
            // ── Active shipment (nullable — order may have no shipment yet)
            Long shipmentId, String courierName, String trackingNumber,
            String shipmentStatus, LocalDateTime shippedAt, LocalDateTime deliveredAt
    ) {}

    // =========================================================================
    // 1.  sp_get_customer_order_history
    // =========================================================================

    /**
     * Executes {@code sp_get_customer_order_history} and maps every result-set
     * row, capturing the {@code COUNT(*) OVER()} total from the first row so
     * the caller never needs a separate COUNT query.
     *
     * @param customerId  the authenticated customer's ID
     * @param limit       page size (1–100)
     * @param offset      zero-based row offset
     * @param orderStatus optional ENUM filter; pass {@code null} for no filter
     * @return {@link OrderHistoryResult} with the page rows and total count
     */
    public OrderHistoryResult callGetCustomerOrderHistory(
            Long customerId,
            int limit,
            int offset,
            String orderStatus
    ) {
        log.debug("sp_get_customer_order_history › customerId={}, limit={}, offset={}, status={}",
                customerId, limit, offset, orderStatus);

        // Mutable total captured from the window-function column on the first row
        final long[] totalHolder = {0L};

        List<OrderSummaryDTO> rows = jdbcTemplate.query(
                "CALL ecom.sp_get_customer_order_history(?, ?, ?, ?)",
                ps -> {
                    ps.setLong(1, customerId);
                    ps.setInt(2, limit);
                    ps.setInt(3, offset);
                    if (orderStatus != null) {
                        ps.setString(4, orderStatus);
                    } else {
                        ps.setNull(4, Types.VARCHAR);
                    }
                },
                (rs, rowNum) -> {
                    if (rowNum == 0) {
                        totalHolder[0] = rs.getLong("total_matching_orders");
                    }
                    return mapOrderSummary(rs);
                }
        );

        return new OrderHistoryResult(rows, totalHolder[0]);
    }

    /**
     * Executes {@code sp_get_customer_order_history_report} to fetch all matching
     * order rows for the specified date range (year/month/day), status, and order number.
     *
     * @param customerId  the customer ID
     * @param startDate   optional start date filter (inclusive)
     * @param endDate     optional end date filter (inclusive)
     * @param orderStatus optional order status filter
     * @param orderNo     optional order number search query
     * @return List of {@link OrderHistoryReportDTO}
     */
    public List<OrderHistoryReportDTO> callGetCustomerOrderHistoryReport(
            Long customerId,
            LocalDate startDate,
            LocalDate endDate,
            String orderStatus,
            String orderNo
    ) {
        log.debug("sp_get_customer_order_history_report › customerId={}, start={}, end={}, status={}, orderNo={}",
                customerId, startDate, endDate, orderStatus, orderNo);

        return jdbcTemplate.query(
                "CALL ecom.sp_get_customer_order_history_report(?, ?, ?, ?, ?)",
                ps -> {
                    ps.setLong(1, customerId);
                    if (startDate != null) {
                        ps.setDate(2, java.sql.Date.valueOf(startDate));
                    } else {
                        ps.setNull(2, Types.DATE);
                    }
                    if (endDate != null) {
                        ps.setDate(3, java.sql.Date.valueOf(endDate));
                    } else {
                        ps.setNull(3, Types.DATE);
                    }
                    if (orderStatus != null && !orderStatus.isBlank()) {
                        ps.setString(4, orderStatus);
                    } else {
                        ps.setNull(4, Types.VARCHAR);
                    }
                    if (orderNo != null && !orderNo.isBlank()) {
                        ps.setString(5, orderNo);
                    } else {
                        ps.setNull(5, Types.VARCHAR);
                    }
                },
                (rs, rowNum) -> mapOrderHistoryReportRow(rs)
        );
    }

    // =========================================================================
    // 2.  sp_get_order_invoice_detail  (two result sets)
    // =========================================================================

    /**
     * Executes {@code sp_get_order_invoice_detail} and processes both result
     * sets within a single JDBC connection.
     *
     * <p>We acquire the connection from {@link DataSource} directly because
     * {@link JdbcTemplate} does not expose a public contract for iterating
     * multiple result sets via {@link Statement#getMoreResults()}.</p>
     *
     * @param orderId    the order to retrieve
     * @param customerId ownership token — must match the order's {@code customer_id}
     * @return {@link InvoiceRawResult} with the header row and line-item list
     * @throws org.springframework.dao.DataAccessException wrapping any JDBC /
     *         MySQL {@code SIGNAL} error
     */
    public InvoiceRawResult callGetOrderInvoiceDetail(Long orderId, Long customerId) {
        log.debug("sp_get_order_invoice_detail › orderId={}, customerId={}", orderId, customerId);

        // Use try-with-resources to guarantee connection and statement are closed
        try (Connection con = dataSource.getConnection();
             CallableStatement cs = con.prepareCall(
                     "CALL ecom.sp_get_order_invoice_detail(?, ?)")) {

            cs.setLong(1, orderId);
            cs.setLong(2, customerId);

            // execute() returns true when the first result is a ResultSet
            boolean hasResultSet = cs.execute();

            // ── Result Set 1: Order header ─────────────────────────────────
            InvoiceHeaderRow headerRow = null;
            if (hasResultSet) {
                ResultSet rs1 = cs.getResultSet();
                if (rs1 != null) {
                    try {
                        if (rs1.next()) {
                            headerRow = mapInvoiceHeader(rs1);
                        }
                    } finally {
                        rs1.close();
                    }
                }
            }

            // ── Result Set 2: Order line items ─────────────────────────────
            List<OrderItemDetailDTO> items = new ArrayList<>();
            // getMoreResults() advances to the next result; returns true if it's a ResultSet
            if (cs.getMoreResults()) {
                ResultSet rs2 = cs.getResultSet();
                if (rs2 != null) {
                    try {
                        while (rs2.next()) {
                            items.add(mapOrderItemDetail(rs2));
                        }
                    } finally {
                        rs2.close();
                    }
                }
            }

            return new InvoiceRawResult(
                    Optional.ofNullable(headerRow),
                    Collections.unmodifiableList(items)
            );

        } catch (SQLException ex) {
            /*
             * Re-wrap as Spring's DataAccessException hierarchy so the service
             * layer can handle MySQL SIGNAL errors uniformly via
             * DataAccessException.getMostSpecificCause().
             */
            throw jdbcTemplate.getExceptionTranslator()
                    .translate("callGetOrderInvoiceDetail", null, ex);
        }
    }

    // =========================================================================
    // Private mapping helpers
    // =========================================================================

    /** Maps one row from the order-history result set to an {@link OrderSummaryDTO}. */
    private OrderSummaryDTO mapOrderSummary(ResultSet rs) throws SQLException {
        return OrderSummaryDTO.builder()
                .orderId(rs.getLong("order_id"))
                .orderNo(rs.getString("order_no"))
                .orderStatus(rs.getString("order_status"))
                .paymentStatus(rs.getString("payment_status"))
                .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                .subtotalAmount(rs.getBigDecimal("subtotal_amount"))
                .shippingFee(rs.getBigDecimal("shipping_fee"))
                .taxAmount(rs.getBigDecimal("tax_amount"))
                .discountAmount(rs.getBigDecimal("discount_amount"))
                .totalAmount(rs.getBigDecimal("total_amount"))
                .totalLineItems(rs.getInt("total_line_items"))
                .totalItemQuantity(rs.getInt("total_item_quantity"))
                .latestPaymentMethod(rs.getString("latest_payment_method"))
                .build();
    }

    /** Maps one row from the order-history report result set to an {@link OrderHistoryReportDTO}. */
    private OrderHistoryReportDTO mapOrderHistoryReportRow(ResultSet rs) throws SQLException {
        LocalDateTime dt = toLocalDateTime(rs.getTimestamp("order_date"));
        return OrderHistoryReportDTO.builder()
                .orderId(rs.getLong("order_id"))
                .orderNo(rs.getString("order_no"))
                .orderDate(dt)
                .orderDateStr(dt != null ? dt.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a")) : "")
                .orderStatus(rs.getString("order_status"))
                .paymentStatus(rs.getString("payment_status"))
                .paymentMethod(rs.getString("payment_method"))
                .itemsSummary(rs.getString("items_summary"))
                .totalItemQuantity(rs.getInt("total_item_quantity"))
                .subtotalAmount(rs.getBigDecimal("subtotal_amount"))
                .shippingFee(rs.getBigDecimal("shipping_fee"))
                .discountAmount(rs.getBigDecimal("discount_amount"))
                .totalAmount(rs.getBigDecimal("total_amount"))
                .customerName(rs.getString("customer_name"))
                .customerEmail(rs.getString("customer_email"))
                .customerPhone(rs.getString("customer_phone"))
                .build();
    }

    /** Maps the single header row from Result Set 1 to an {@link InvoiceHeaderRow}. */
    private InvoiceHeaderRow mapInvoiceHeader(ResultSet rs) throws SQLException {
        return new InvoiceHeaderRow(
                // Order header
                rs.getLong("order_id"),
                rs.getString("order_no"),
                rs.getString("order_status"),
                rs.getString("payment_status"),
                toLocalDateTime(rs.getTimestamp("order_placed_at")),
                toLocalDateTime(rs.getTimestamp("order_modified_at")),
                rs.getBigDecimal("subtotal_amount"),
                rs.getBigDecimal("shipping_fee"),
                rs.getBigDecimal("tax_amount"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("total_amount"),
                // Customer
                rs.getLong("customer_id"),
                rs.getString("customer_name"),
                rs.getString("customer_email"),
                rs.getString("customer_phone"),
                // Address
                rs.getString("recipient_name"),
                rs.getString("recipient_phone"),
                rs.getString("address_line1"),
                rs.getString("address_line2"),
                rs.getString("township"),
                rs.getString("city"),
                rs.getString("region_or_state"),
                rs.getString("full_address_text"),
                // Payment (nullable)
                nullableLong(rs, "payment_id"),
                rs.getString("payment_method"),
                rs.getString("latest_payment_status"),
                rs.getString("transaction_ref"),
                toLocalDateTime(rs.getTimestamp("paid_at")),
                rs.getBigDecimal("latest_payment_amount"),
                // Shipment (nullable)
                nullableLong(rs, "shipment_id"),
                rs.getString("courier_name"),
                rs.getString("tracking_number"),
                rs.getString("shipment_status"),
                toLocalDateTime(rs.getTimestamp("shipped_at")),
                toLocalDateTime(rs.getTimestamp("delivered_at"))
        );
    }

    /** Maps one row from Result Set 2 to an {@link OrderItemDetailDTO}. */
    private OrderItemDetailDTO mapOrderItemDetail(ResultSet rs) throws SQLException {
        return OrderItemDetailDTO.builder()
                .orderItemId(rs.getLong("order_item_id"))
                .productName(rs.getString("product_name"))
                .sku(rs.getString("sku"))
                .variantAttributes(rs.getString("variant_attributes"))
                .unitPrice(rs.getBigDecimal("unit_price"))
                .qty(rs.getInt("qty"))
                .discountAmount(rs.getBigDecimal("discount_amount"))
                .subtotal(rs.getBigDecimal("subtotal"))
                .build();
    }

    // ── Type-conversion utilities ──────────────────────────────────────────

    /** Null-safe {@link Timestamp} → {@link LocalDateTime} conversion. */
    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    /**
     * Safely reads a potentially-NULL {@code BIGINT} column.
     * {@link ResultSet#getLong} returns {@code 0} for SQL NULL, so we verify
     * with {@link ResultSet#wasNull()} immediately after the read.
     */
    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long val = rs.getLong(column);
        return rs.wasNull() ? null : val;
    }
}
