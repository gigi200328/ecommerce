package com.ojt.ecommerce.storefront.order.service;

import com.ojt.ecommerce.storefront.order.dto.CustomerOrderHistoryResponseDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderHistoryRequest;
import com.ojt.ecommerce.storefront.order.dto.OrderInvoiceResponseDTO;

import java.time.LocalDate;

/**
 * Read-only service interface for the customer-facing order inspection operations.
 * All implementations MUST use {@code @Transactional(readOnly = true)}.
 */
public interface OrderInspectionService {

    /**
     * Returns a paginated, optionally-filtered summary of orders placed by
     * the given customer.
     *
     * @param customerId the authenticated customer's ID
     * @param request    pagination and filter parameters
     * @return paginated order history with financial breakdowns
     */
    CustomerOrderHistoryResponseDTO getCustomerOrderHistory(Long customerId, OrderHistoryRequest request);

    /**
     * Returns a comprehensive invoice for a single order after validating
     * that the order belongs to the authenticated customer.
     *
     * @param orderId    the target order's ID
     * @param customerId ownership token
     * @return full invoice DTO (two logical sections)
     */
    OrderInvoiceResponseDTO getOrderInvoiceDetail(Long orderId, Long customerId);

    /**
     * Generates and returns the invoice PDF bytes for the given order.
     * Internally calls {@link #getOrderInvoiceDetail} and fills the
     * {@code order_invoice.jrxml} compiled report template.
     *
     * @param orderId    the target order's ID
     * @param customerId ownership token (IDOR guard)
     * @return raw PDF byte array ready to stream in an HTTP response
     */
    byte[] exportInvoicePdf(Long orderId, Long customerId);

    /**
     * Generates and returns the PDF bytes for the customer order history report
     * based on the given date filters (year/month/day), status, and order number.
     *
     * @param customerId  ownership token (IDOR guard)
     * @param startDate   optional from date filter (inclusive)
     * @param endDate     optional to date filter (inclusive)
     * @param orderStatus optional order status filter
     * @param orderNo     optional order number search query
     * @return raw PDF byte array ready to stream in an HTTP response
     */
    byte[] exportOrderHistoryPdf(Long customerId, LocalDate startDate, LocalDate endDate, String orderStatus, String orderNo);
}
