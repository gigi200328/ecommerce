package com.ojt.ecommerce.storefront.order.controller;

import com.ojt.ecommerce.storefront.order.dto.ApiResponse;
import com.ojt.ecommerce.storefront.order.dto.CustomerOrderHistoryResponseDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderHistoryRequest;
import com.ojt.ecommerce.storefront.order.dto.OrderInvoiceResponseDTO;
import com.ojt.ecommerce.storefront.order.service.OrderInspectionService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for read-only customer order inspection.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /api/v1/customers/{customerId}/orders}
 *       — paginated order history for the authenticated customer.</li>
 *   <li>{@code GET /api/v1/orders/{orderId}/invoice?customerId={id}}
 *       — full invoice detail with ownership verification.</li>
 * </ul>
 *
 * <h3>Security Note</h3>
 * <p>The {@code customerId} path/query parameter is always cross-checked against
 * the JWT-authenticated {@link CustomerPrincipal#getCustomerId()} before the
 * request is forwarded to the service. This prevents Insecure Direct Object
 * Reference (IDOR) attacks where a user supplies another customer's ID.</p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping({"/api/storefront/v1", "/api/v1"})
@RequiredArgsConstructor
public class OrderInspectionController {

    private final OrderInspectionService orderInspectionService;

    // =========================================================================
    // GET /customers/{customerId}/orders
    // =========================================================================

    /**
     * Returns a paginated, optionally-filtered list of orders placed by the
     * authenticated customer.
     *
     * @param customerId  path variable — must match the authenticated customer
     * @param principal   JWT-injected identity of the caller
     * @param request     validated query parameters
     * @return {@code 200 OK} with paginated order summaries
     */
    @GetMapping("/customers/{customerId}/orders")
    public ResponseEntity<ApiResponse<CustomerOrderHistoryResponseDTO>> getCustomerOrderHistory(
            @PathVariable @Positive(message = "customerId must be a positive number") Long customerId,
            @AuthenticationPrincipal CustomerPrincipal principal,
            @Valid @ModelAttribute OrderHistoryRequest request
    ) {
        Long effectiveCustomerId = resolveCustomerId(principal, customerId);

        log.info("Order history request — customerId={}, limit={}, offset={}, status={}",
                effectiveCustomerId, request.getLimit(), request.getOffset(), request.getOrderStatus());

        CustomerOrderHistoryResponseDTO response =
                orderInspectionService.getCustomerOrderHistory(effectiveCustomerId, request);

        return ResponseEntity.ok(ApiResponse.ok("Order history retrieved successfully.", response));
    }

    // =========================================================================
    // GET /orders/{orderId}/invoice  (or /orders/{orderId}/receipt)
    // =========================================================================

    /**
     * Returns the full invoice / receipt for a single order.
     *
     * @param orderId    path variable — the target order
     * @param customerId query parameter (optional when authenticated via JWT)
     * @param principal  JWT-injected identity of the caller
     * @return {@code 200 OK} with full invoice DTO
     */
    @GetMapping({"/orders/{orderId}/invoice", "/orders/{orderId}/receipt"})
    public ResponseEntity<ApiResponse<OrderInvoiceResponseDTO>> getOrderInvoiceDetail(
            @PathVariable @Positive(message = "orderId must be a positive number") Long orderId,
            @RequestParam(required = false) Long customerId,
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        Long effectiveCustomerId = resolveCustomerId(principal, customerId);

        log.info("Invoice detail request — orderId={}, customerId={}", orderId, effectiveCustomerId);

        OrderInvoiceResponseDTO response =
                orderInspectionService.getOrderInvoiceDetail(orderId, effectiveCustomerId);

        return ResponseEntity.ok(ApiResponse.ok("Invoice detail retrieved successfully.", response));
    }

    // =========================================================================
    // GET /orders/{orderId}/invoice/pdf  (or /orders/{orderId}/receipt/pdf)
    // =========================================================================

    /**
     * Downloads a PDF-rendered invoice for the given order using JasperReports.
     *
     * @param orderId    path variable — the target order
     * @param customerId query parameter (optional when authenticated via JWT)
     * @param principal  JWT-injected identity of the caller
     * @return {@code 200 OK} with {@code application/pdf} body
     */
    @GetMapping({"/orders/{orderId}/invoice/pdf", "/orders/{orderId}/receipt/pdf"})
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable @Positive(message = "orderId must be a positive number") Long orderId,
            @RequestParam(required = false) Long customerId,
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        Long effectiveCustomerId = resolveCustomerId(principal, customerId);

        log.info("Invoice PDF download request — orderId={}, customerId={}", orderId, effectiveCustomerId);

        byte[] pdf = orderInspectionService.exportInvoicePdf(orderId, effectiveCustomerId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("invoice-" + orderId + ".pdf")
                .build());
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    // =========================================================================
    // GET /orders/report/pdf
    // =========================================================================

    /**
     * Downloads a PDF-rendered report of customer order history matching the
     * specified date filters (year/month/day), status, and order number.
     *
     * @param customerId  query parameter (optional when authenticated via JWT)
     * @param startDate   optional from date filter (inclusive)
     * @param endDate     optional to date filter (inclusive)
     * @param orderStatus optional order status filter
     * @param orderNo     optional order number search query
     * @param principal   JWT-injected identity of the caller
     * @return {@code 200 OK} with {@code application/pdf} body
     */
    @GetMapping({"/orders/report/pdf", "/customers/{customerId}/orders/report/pdf"})
    public ResponseEntity<byte[]> downloadOrderHistoryPdf(
            @PathVariable(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String orderNo,
            @AuthenticationPrincipal CustomerPrincipal principal
    ) {
        Long effectiveCustomerId = resolveCustomerId(principal, customerId);

        log.info("Order history report PDF download request — customerId={}, start={}, end={}, status={}, orderNo={}",
                effectiveCustomerId, startDate, endDate, orderStatus, orderNo);

        byte[] pdf = orderInspectionService.exportOrderHistoryPdf(
                effectiveCustomerId, startDate, endDate, orderStatus, orderNo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("order-history-report.pdf")
                .build());
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves and verifies customer ownership. If the caller is authenticated via JWT,
     * their principal ID takes precedence. If an explicit customerId was requested,
     * it must match the principal ID.
     */
    private Long resolveCustomerId(CustomerPrincipal principal, Long requestedCustomerId) {
        if (principal != null) {
            if (requestedCustomerId != null && !principal.getCustomerId().equals(requestedCustomerId)) {
                log.warn("IDOR attempt — authenticated={}, requested={}",
                        principal.getCustomerId(), requestedCustomerId);
                throw new com.ojt.ecommerce.storefront.exception.AccessDeniedException(
                        "You are not authorised to access this resource.");
            }
            return principal.getCustomerId();
        }
        if (requestedCustomerId != null) {
            return requestedCustomerId;
        }
        throw new com.ojt.ecommerce.storefront.exception.AccessDeniedException(
                "Customer authentication required.");
    }
}
