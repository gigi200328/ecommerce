package com.ojt.ecommerce.storefront.order.service;

import com.ojt.ecommerce.storefront.exception.AccessDeniedException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.order.dto.CustomerOrderHistoryResponseDTO;
import com.ojt.ecommerce.storefront.order.dto.CustomerOrderHistoryResponseDTO.OrderSummaryDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderHistoryRequest;
import com.ojt.ecommerce.storefront.order.dto.OrderInvoiceResponseDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderInvoiceResponseDTO.PaymentInfoDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderInvoiceResponseDTO.ShipmentTrackingDTO;
import com.ojt.ecommerce.storefront.order.dto.OrderInvoiceResponseDTO.ShippingAddressDTO;
import com.ojt.ecommerce.storefront.order.repository.OrderInspectionRepository;
import com.ojt.ecommerce.storefront.order.repository.OrderInspectionRepository.InvoiceHeaderRow;
import com.ojt.ecommerce.storefront.order.repository.OrderInspectionRepository.InvoiceRawResult;
import com.ojt.ecommerce.storefront.order.repository.OrderInspectionRepository.OrderHistoryResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ojt.ecommerce.storefront.order.dto.OrderHistoryReportDTO;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Production implementation of {@link OrderInspectionService}.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li><b>Input sanitisation</b> — strips whitespace, normalises nulls before
 *       delegating to the repository layer.</li>
 *   <li><b>Error translation</b> — MySQL {@code SIGNAL} errors (SQLSTATE 45000)
 *       raised inside the stored procedures are surfaced through
 *       {@link DataAccessException} and translated to domain exceptions that
 *       the global handler maps to the correct HTTP status codes.</li>
 *   <li><b>DTO assembly</b> — flat repository rows are structured into nested
 *       response DTOs consumed by the REST controller.</li>
 *   <li><b>Pagination metadata</b> — total pages, current page, and first/last
 *       flags are computed from the window-function count embedded in the SP
 *       result (no extra COUNT query needed).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderInspectionServiceImpl implements OrderInspectionService {

    private final OrderInspectionRepository repository;

    // =========================================================================
    // 1.  Customer Order History
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public CustomerOrderHistoryResponseDTO getCustomerOrderHistory(
            Long customerId,
            OrderHistoryRequest request
    ) {
        // ── 1. Sanitise pagination parameters ─────────────────────────────
        int limit  = resolveLimit(request.getLimit());
        int offset = resolveOffset(request.getOffset());
        String status = sanitiseStatus(request.getOrderStatus());

        // ── 2. Execute stored procedure ────────────────────────────────────
        OrderHistoryResult result;
        try {
            result = repository.callGetCustomerOrderHistory(customerId, limit, offset, status);
        } catch (DataAccessException ex) {
            /*
             * MySQL SIGNAL errors (SQLSTATE 45000) surface as DataAccessException.
             * The SP validates customer existence and status enum values internally,
             * so we translate the message into the appropriate domain exception.
             */
            String msg = extractMySqlSignalMessage(ex);
            log.warn("SP validation failure — customerId={}: {}", customerId, msg);
            throw new ResourceNotFoundException(msg);
        }

        List<OrderSummaryDTO> rows = result.orders();

        // ── 3. Handle empty result ─────────────────────────────────────────
        if (rows.isEmpty()) {
            return buildEmptyHistoryResponse(limit, offset);
        }

        // ── 4. Compute pagination metadata from the SP window count ────────
        long totalItems = result.totalMatchingOrders();
        int  totalPages  = (int) Math.ceil((double) totalItems / limit);
        int  currentPage = offset / limit;     // zero-based page index
        boolean isFirst  = (offset == 0);
        boolean isLast   = (offset + limit >= totalItems);

        // ── 5. Assemble and return response ───────────────────────────────
        return CustomerOrderHistoryResponseDTO.builder()
                .totalItems(totalItems)
                .totalPages(totalPages)
                .currentPage(currentPage)
                .pageSize(limit)
                .first(isFirst)
                .last(isLast)
                .orders(rows)
                .build();
    }

    // =========================================================================
    // 2.  Order Invoice Detail
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public OrderInvoiceResponseDTO getOrderInvoiceDetail(Long orderId, Long customerId) {

        // ── 1. Execute stored procedure (ownership check is inside the SP) ─
        InvoiceRawResult raw;
        try {
            raw = repository.callGetOrderInvoiceDetail(orderId, customerId);
        } catch (DataAccessException ex) {
            String msg = extractMySqlSignalMessage(ex);
            log.warn("SP validation/access failure — orderId={}, customerId={}: {}", orderId, customerId, msg);
            if (msg.contains("Access Denied")) {
                throw new AccessDeniedException(msg);
            }
            throw new ResourceNotFoundException(msg);
        }

        // ── 2. Unpack Result Set 1: order header ───────────────────────────
        InvoiceHeaderRow header = raw.header()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found or does not belong to this customer."));

        ShippingAddressDTO address = ShippingAddressDTO.builder()
                .recipientName(header.recipientName())
                .recipientPhone(header.recipientPhone())
                .addressLine1(header.addressLine1())
                .addressLine2(header.addressLine2())
                .township(header.township())
                .city(header.city())
                .regionOrState(header.regionOrState())
                .fullAddressText(header.fullAddressText())
                .build();

        PaymentInfoDTO payment      = buildPaymentInfo(header);
        ShipmentTrackingDTO shipment = buildShipmentTracking(header);

        // ── 3. Unpack Result Set 2: line items (already mapped) ────────────
        List<OrderInvoiceResponseDTO.OrderItemDetailDTO> items =
                raw.items() != null ? raw.items() : Collections.emptyList();

        // ── 4. Assemble final response ─────────────────────────────────────
        return OrderInvoiceResponseDTO.builder()
                .orderId(header.orderId())
                .orderNo(header.orderNo())
                .orderStatus(header.orderStatus())
                .paymentStatus(header.paymentStatus())
                .orderPlacedAt(header.orderPlacedAt())
                .orderModifiedAt(header.orderModifiedAt())
                .subtotalAmount(header.subtotalAmount())
                .shippingFee(header.shippingFee())
                .taxAmount(header.taxAmount())
                .discountAmount(header.discountAmount())
                .totalAmount(header.totalAmount())
                .customerId(header.customerId())
                .customerName(header.customerName())
                .customerEmail(header.customerEmail())
                .customerPhone(header.customerPhone())
                .shippingAddress(address)
                .paymentInfo(payment)
                .shipmentTracking(shipment)
                .items(items)
                .build();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private int resolveLimit(Integer raw) {
        if (raw == null || raw <= 0) return 20;
        return Math.min(raw, 100);
    }

    private int resolveOffset(Integer raw) {
        if (raw == null || raw < 0) return 0;
        return raw;
    }

    /** Trims and upper-cases the status; returns null for blank/missing values. */
    private String sanitiseStatus(String raw) {
        return StringUtils.hasText(raw) ? raw.trim().toUpperCase() : null;
    }

    /**
     * Builds a {@link PaymentInfoDTO} only when the SP returned payment data.
     * A null paymentId indicates that no payment record exists for this order.
     */
    private PaymentInfoDTO buildPaymentInfo(InvoiceHeaderRow h) {
        if (h.paymentId() == null) return null;
        return PaymentInfoDTO.builder()
                .paymentId(h.paymentId())
                .paymentMethod(h.paymentMethod())
                .paymentStatus(h.latestPaymentStatus())
                .transactionRef(h.transactionRef())
                .paidAt(h.paidAt())
                .amount(h.paymentAmount())
                .build();
    }

    /**
     * Builds a {@link ShipmentTrackingDTO} only when the SP returned an
     * active shipment record.  A null shipmentId means no shipment exists yet.
     */
    private ShipmentTrackingDTO buildShipmentTracking(InvoiceHeaderRow h) {
        if (h.shipmentId() == null) return null;
        return ShipmentTrackingDTO.builder()
                .shipmentId(h.shipmentId())
                .courierName(h.courierName())
                .trackingNumber(h.trackingNumber())
                .shipmentStatus(h.shipmentStatus())
                .shippedAt(h.shippedAt())
                .deliveredAt(h.deliveredAt())
                .build();
    }

    /** Returns an empty paginated response when the customer has no matching orders. */
    private CustomerOrderHistoryResponseDTO buildEmptyHistoryResponse(int limit, int offset) {
        return CustomerOrderHistoryResponseDTO.builder()
                .totalItems(0)
                .totalPages(0)
                .currentPage(limit > 0 ? offset / limit : 0)
                .pageSize(limit)
                .first(true)
                .last(true)
                .orders(Collections.emptyList())
                .build();
    }

    /**
     * Extracts the human-readable message from a MySQL SIGNAL exception.
     * The MySQL Connector/J driver wraps the SIGNAL message as the root cause.
     */
    private String extractMySqlSignalMessage(DataAccessException ex) {
        Throwable cause = ex.getMostSpecificCause();
        return (cause != null && StringUtils.hasText(cause.getMessage()))
                ? cause.getMessage()
                : ex.getMessage();
    }

    // =========================================================================
    // 3.  PDF Export (Jasper Reports)
    // =========================================================================

    /**
     * Compiles (pre-compiled .jasper) and fills {@code order_invoice.jrxml}
     * using the invoice DTO data, then exports to a PDF byte array.
     *
     * <h3>Classpath layout expected</h3>
     * <pre>
     *   src/main/resources/reports/
     *     order_invoice.jasper          (compiled from order_invoice.jrxml)
     *     order_invoice_items.jasper    (compiled from order_invoice_items.jrxml)
     * </pre>
     *
     * <p><b>Tip:</b> Compile .jrxml → .jasper once at build time using
     * the {@code jasperreports-maven-plugin} so you never re-compile at
     * runtime.</p>
     */
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a");

    @Override
    @Transactional(readOnly = true)
    public byte[] exportInvoicePdf(Long orderId, Long customerId) {
        // 1. Fetch the data (IDOR-safe — SP validates ownership)
        OrderInvoiceResponseDTO dto = getOrderInvoiceDetail(orderId, customerId);

        try {
            // 2. Load pre-compiled or compile single invoice report (no subreport needed)
            JasperReport report = loadOrCompileReport("order_invoice");

            // 3. Line items datasource
            List<OrderInvoiceResponseDTO.OrderItemDetailDTO> items =
                    (dto.getItems() != null) ? dto.getItems() : Collections.emptyList();
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(items);

            // 4. Fill parameters
            Map<String, Object> params = new HashMap<>();
            params.put("LOGO_IMAGE", loadLogoImage());
            params.put("ORDER_NO", dto.getOrderNo());
            params.put("ORDER_DATE_STR", dto.getOrderPlacedAt() != null ? dto.getOrderPlacedAt().format(DATE_FMT) : "");
            params.put("CUSTOMER_NAME", dto.getCustomerName());

            ShippingAddressDTO addr = dto.getShippingAddress();
            if (addr != null) {
                params.put("RECIPIENT_NAME", addr.getRecipientName());
                params.put("RECIPIENT_PHONE", addr.getRecipientPhone());
                params.put("ADDRESS_LINE1", addr.getAddressLine1());
                String townshipCity = Stream.of(addr.getTownship(), addr.getCity())
                        .filter(StringUtils::hasText)
                        .collect(Collectors.joining(", "));
                params.put("TOWNSHIP_CITY", townshipCity);
                params.put("REGION_STATE", addr.getRegionOrState());
            } else {
                params.put("RECIPIENT_NAME", "");
                params.put("RECIPIENT_PHONE", "");
                params.put("ADDRESS_LINE1", "");
                params.put("TOWNSHIP_CITY", "");
                params.put("REGION_STATE", "");
            }

            PaymentInfoDTO payment = dto.getPaymentInfo();
            if (payment != null) {
                params.put("PAYMENT_METHOD", payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "N/A");
                params.put("PAYMENT_STATUS", payment.getPaymentStatus() != null ? payment.getPaymentStatus() : "PENDING");
                params.put("TRANSACTION_REF", payment.getTransactionRef() != null ? payment.getTransactionRef() : "-");
                params.put("PAID_AT_STR", payment.getPaidAt() != null ? payment.getPaidAt().format(DATE_FMT) : "-");
            } else {
                params.put("PAYMENT_METHOD", "N/A");
                params.put("PAYMENT_STATUS", "PENDING");
                params.put("TRANSACTION_REF", "-");
                params.put("PAID_AT_STR", "-");
            }

            params.put("SUBTOTAL_AMOUNT", dto.getSubtotalAmount() != null ? dto.getSubtotalAmount() : BigDecimal.ZERO);
            params.put("SHIPPING_FEE", dto.getShippingFee() != null ? dto.getShippingFee() : BigDecimal.ZERO);
            params.put("TOTAL_AMOUNT", dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO);

            // 5. Fill report
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, ds);

            // 6. Export to PDF bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
            exporter.exportReport();

            log.info("Invoice PDF generated successfully — orderId={}, size={} bytes",
                    orderId, out.size());
            return out.toByteArray();

        } catch (Exception ex) {
            log.error("Failed to generate invoice PDF for orderId={}: {}", orderId, ex.getMessage(), ex);
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            throw new RuntimeException("Invoice PDF generation failed: " + root.getMessage(), ex);
        }
    }

    // =========================================================================
    // 4.  Customer Order History Report PDF (Jasper Reports)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOrderHistoryPdf(
            Long customerId,
            LocalDate startDate,
            LocalDate endDate,
            String orderStatus,
            String orderNo
    ) {
        String cleanStatus = sanitiseStatus(orderStatus);
        String cleanOrderNo = StringUtils.hasText(orderNo) ? orderNo.trim() : null;

        // 1. Fetch report rows from Stored Procedure
        List<OrderHistoryReportDTO> rows;
        try {
            rows = repository.callGetCustomerOrderHistoryReport(
                    customerId, startDate, endDate, cleanStatus, cleanOrderNo);
        } catch (DataAccessException ex) {
            String msg = extractMySqlSignalMessage(ex);
            log.warn("Order history report SP failed — customerId={}: {}", customerId, msg);
            throw new ResourceNotFoundException(msg);
        }

        try {
            // 2. Load or compile order_history_report.jrxml
            JasperReport report = loadOrCompileReport("order_history_report");

            // 3. Setup parameters
            Map<String, Object> params = new HashMap<>();
            params.put("LOGO_IMAGE", loadLogoImage());

            // Customer info (from first row if available, else fallback)
            String customerName = "Valued Customer";
            String customerEmail = "";
            if (!rows.isEmpty()) {
                if (StringUtils.hasText(rows.get(0).getCustomerName())) {
                    customerName = rows.get(0).getCustomerName();
                }
                if (StringUtils.hasText(rows.get(0).getCustomerEmail())) {
                    customerEmail = rows.get(0).getCustomerEmail();
                }
            }
            params.put("CUSTOMER_NAME", customerName);
            params.put("CUSTOMER_EMAIL", customerEmail);

            // Filter period display string
            String periodStr;
            if (startDate != null && endDate != null) {
                if (startDate.equals(endDate)) {
                    periodStr = "Date: " + startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
                } else {
                    periodStr = "From: " + startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                            + " To: " + endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
                }
            } else if (startDate != null) {
                periodStr = "From: " + startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            } else if (endDate != null) {
                periodStr = "Up to: " + endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            } else {
                periodStr = "All Time";
            }
            params.put("PERIOD_STR", periodStr);

            // Filter status display string
            params.put("STATUS_FILTER_STR", cleanStatus != null ? cleanStatus : "ALL");

            // Search query display string
            params.put("ORDER_NO_FILTER_STR", cleanOrderNo != null ? cleanOrderNo : "ALL");

            // Generation timestamp
            params.put("GENERATED_AT_STR", LocalDateTime.now().format(DATE_FMT));

            // Summary metrics
            BigDecimal grandTotal = rows.stream()
                    .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            params.put("TOTAL_ORDERS_COUNT", rows.size());
            params.put("GRAND_TOTAL_AMOUNT", grandTotal);

            // 4. Line items datasource
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(rows);

            // 5. Fill report
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, ds);

            // 6. Export to PDF bytes
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
            exporter.exportReport();

            log.info("Order history report PDF generated — customerId={}, orders={}, size={} bytes",
                    customerId, rows.size(), out.size());
            return out.toByteArray();

        } catch (Exception ex) {
            log.error("Failed to generate order history PDF for customerId={}: {}", customerId, ex.getMessage(), ex);
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            throw new RuntimeException("Order history PDF generation failed: " + root.getMessage(), ex);
        }
    }

    /**
     * Loads a compiled .jasper report from classpath, or falls back to
     * compiling the source .jrxml dynamically if the .jasper file is absent.
     */
    private JasperReport loadOrCompileReport(String baseName) throws Exception {
        // 1. Check for pre-compiled .jasper
        InputStream jasperStream = getClass()
                .getClassLoader()
                .getResourceAsStream("reports/" + baseName + ".jasper");
        if (jasperStream != null) {
            try (jasperStream) {
                log.debug("Loaded pre-compiled report: reports/{}.jasper", baseName);
                return (JasperReport) JRLoader.loadObject(jasperStream);
            }
        }

        // 2. Fallback: compile .jrxml source at runtime
        InputStream jrxmlStream = getClass()
                .getClassLoader()
                .getResourceAsStream("reports/" + baseName + ".jrxml");
        if (jrxmlStream != null) {
            try (jrxmlStream) {
                log.info("Compiling report from source: reports/{}.jrxml", baseName);
                return JasperCompileManager.compileReport(jrxmlStream);
            }
        }

        throw new IllegalStateException(
                "Neither reports/" + baseName + ".jasper nor reports/" + baseName + ".jrxml found on classpath.");
    }

    private static final String UPLOADED_LOGO_PATH =
            "C:/Users/User/.gemini/antigravity/brain/d5928173-e63b-411d-b3c8-5f4c967aeb95/.user_uploaded/media_1792946063716_87f1dbaa.png";

    private byte[] cachedLogoBytes = null;

    @PostConstruct
    public void init() {
        loadLogoImage();
    }

    /**
     * Loads the store logo image bytes for JasperReports.
     * Looks up classpath resource reports/logo.png first, followed by local filesystem paths,
     * including the user uploaded logo asset. Automatically saves the image to resources
     * so it persists with the project build.
     */
    private synchronized byte[] loadLogoImage() {
        if (cachedLogoBytes != null) {
            return cachedLogoBytes;
        }

        // 1. Try classpath resource
        try (InputStream is = getClass().getResourceAsStream("/reports/logo.png")) {
            if (is != null) {
                cachedLogoBytes = is.readAllBytes();
                log.debug("Store logo loaded from classpath /reports/logo.png ({} bytes)", cachedLogoBytes.length);
                return cachedLogoBytes;
            }
        } catch (Exception ignored) {
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("reports/logo.png")) {
            if (is != null) {
                cachedLogoBytes = is.readAllBytes();
                log.debug("Store logo loaded from classloader reports/logo.png ({} bytes)", cachedLogoBytes.length);
                return cachedLogoBytes;
            }
        } catch (Exception ignored) {
        }

        // 2. Try filesystem candidate paths
        List<Path> candidates = List.of(
                Paths.get("c:/Users/User/Downloads/ecommerce/ecommerce/src/main/resources/reports/logo.png"),
                Paths.get("src/main/resources/reports/logo.png"),
                Paths.get("ecommerce/src/main/resources/reports/logo.png"),
                Paths.get(UPLOADED_LOGO_PATH)
        );

        for (Path p : candidates) {
            if (Files.exists(p)) {
                try {
                    byte[] bytes = Files.readAllBytes(p);
                    if (bytes.length > 0) {
                        cachedLogoBytes = bytes;
                        log.info("Store logo loaded from filesystem: {} ({} bytes)", p.toAbsolutePath(), bytes.length);
                        saveLogoToResources(bytes);
                        return cachedLogoBytes;
                    }
                } catch (Exception e) {
                    log.warn("Could not read logo image from {}: {}", p, e.getMessage());
                }
            }
        }

        log.warn("Logo image not found in classpath or filesystem candidates.");
        return null;
    }

    /**
     * Persists the logo bytes into the project's resources directory so subsequent
     * builds and JAR packagings have the logo bundled directly on the classpath.
     */
    private void saveLogoToResources(byte[] bytes) {
        List<Path> targets = List.of(
                Paths.get("c:/Users/User/Downloads/ecommerce/ecommerce/src/main/resources/reports/logo.png"),
                Paths.get("src/main/resources/reports/logo.png"),
                Paths.get("target/classes/reports/logo.png")
        );
        for (Path target : targets) {
            try {
                if (!Files.exists(target)) {
                    if (target.getParent() != null) {
                        Files.createDirectories(target.getParent());
                    }
                    Files.write(target, bytes);
                    log.info("Persisted logo image to {}", target.toAbsolutePath());
                }
            } catch (Exception ignored) {
            }
        }
    }
}
