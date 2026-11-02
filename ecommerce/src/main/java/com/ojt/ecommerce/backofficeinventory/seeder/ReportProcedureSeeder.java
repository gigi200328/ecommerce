package com.ojt.ecommerce.backofficeinventory.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Automatically initializes and synchronizes the 6 MySQL Stored Procedures
 * required for Back-Office Reports on application startup.
 *
 * This ensures all team members have the exact same stored procedures in their
 * local MySQL databases immediately after pulling from Git and starting the application.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ReportProcedureSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Starting Back-Office Report Stored Procedures synchronization...");
        try {
            initDailyProductSalesProcedure();
            initCategorySummaryProcedure();
            initTopSellingProductsProcedure();
            initRefundSummaryProcedure();
            initLowStockAlertProcedure();
            initOrderFulfillmentProcedure();
            initProductCatalogExportProcedure();
            initBrandExportProcedure();
            initInventoryExportProcedure();
            initOrdersPageExportProcedure();
            log.info("Successfully initialized all 10 Back-Office Report Stored Procedures.");
        } catch (Exception e) {
            log.error("Error initializing Back-Office Report Stored Procedures: {}", e.getMessage(), e);
        }
    }

    private void initDailyProductSalesProcedure() {
        jdbcTemplate.execute("DROP PROCEDURE IF EXISTS `SP_GetDailyProductSales`");
        String sql = """
            CREATE PROCEDURE `SP_GetDailyProductSales`(
                IN startDate DATE,
                IN endDate DATE
            )
            BEGIN
                SELECT 
                    DATE(o.created_at) AS salesDate,
                    COALESCE(p.product_name, oi.product_name) AS productName,
                    COALESCE(c.category_name, 'Uncategorized') AS categoryName,
                    SUM(oi.qty) AS quantitySold,
                    oi.unit_price AS pricePerUnit,
                    SUM(oi.subtotal) AS totalSales
                FROM orders o
                JOIN order_items oi ON o.order_id = oi.order_id
                LEFT JOIN product_variants pv ON oi.variant_id = pv.variant_id
                LEFT JOIN products p ON pv.product_id = p.product_id
                LEFT JOIN categories c ON p.category_id = c.category_id
                WHERE DATE(o.created_at) BETWEEN startDate AND endDate
                  AND o.order_status != 'CANCELLED'
                GROUP BY DATE(o.created_at), p.product_id, oi.product_name, c.category_name, oi.unit_price
                ORDER BY salesDate DESC, totalSales DESC;
            END
            """;
        jdbcTemplate.execute(sql);
        log.info("Initialized stored procedure: SP_GetDailyProductSales");
    }

    private void initCategorySummaryProcedure() {
        jdbcTemplate.execute("DROP PROCEDURE IF EXISTS `SP_GetCategorySummaryReport`");
        String sql = """
            CREATE PROCEDURE `SP_GetCategorySummaryReport`()
            BEGIN
                SELECT 
                    c.category_name AS categoryName,
                    COALESCE(p.category_name, 'None') AS parentCategory,
                    COALESCE(c.description, '') AS description,
                    COUNT(prod.product_id) AS totalProducts
                FROM categories c
                LEFT JOIN categories p ON c.parent_id = p.category_id
                LEFT JOIN products prod ON prod.category_id = c.category_id
                GROUP BY c.category_id, c.category_name, p.category_name, c.description
                ORDER BY c.category_name ASC;
            END
            """;
        jdbcTemplate.execute(sql);
        log.info("Initialized stored procedure: SP_GetCategorySummaryReport");
    }

    private void initTopSellingProductsProcedure() {
        jdbcTemplate.execute("DROP PROCEDURE IF EXISTS `SP_GetTopSellingProducts`");
        String sql = """
            CREATE PROCEDURE `SP_GetTopSellingProducts`(
                IN limitCount INT
            )
            BEGIN
                SELECT 
                    COALESCE(p.product_name, oi.product_name) AS productName,
                    COALESCE(c.category_name, 'Uncategorized') AS categoryName,
                    MAX(oi.unit_price) AS unitPrice,
                    SUM(oi.qty) AS totalQuantitySold,
                    SUM(oi.subtotal) AS totalRevenue
                FROM order_items oi
                JOIN orders o ON oi.order_id = o.order_id
                LEFT JOIN product_variants pv ON oi.variant_id = pv.variant_id
                LEFT JOIN products p ON pv.product_id = p.product_id
                LEFT JOIN categories c ON p.category_id = c.category_id
                WHERE o.order_status != 'CANCELLED'
                GROUP BY p.product_id, oi.product_name, c.category_name
                ORDER BY totalQuantitySold DESC, totalRevenue DESC
                LIMIT limitCount;
            END
            """;
        jdbcTemplate.execute(sql);
        log.info("Initialized stored procedure: SP_GetTopSellingProducts");
    }

    private void initRefundSummaryProcedure() {
        jdbcTemplate.execute("DROP PROCEDURE IF EXISTS `SP_GetRefundSummaryReport`");
        String sql = """
            CREATE PROCEDURE `SP_GetRefundSummaryReport`(
                IN start_date DATE,
                IN end_date DATE,
                IN search_keyword VARCHAR(100),
                IN status_filter VARCHAR(50)
            )
            BEGIN
                SELECT 
                    rr.refund_id AS refund_id,
                    rr.refund_id AS refundId,
                    o.order_id AS order_id,
                    o.order_id AS orderId,
                    o.order_no AS order_no,
                    o.order_no AS orderNo,
                    c.full_name AS customer_name,
                    c.full_name AS customerName,
                    COALESCE(oi.product_name, 'Product') AS product_name,
                    COALESCE(oi.product_name, 'Product') AS productName,
                    rr.refund_quantity AS refund_quantity,
                    rr.refund_quantity AS refundQuantity,
                    rr.refund_amount AS refund_amount,
                    rr.refund_amount AS refundAmount,
                    rr.request_reason AS reason,
                    rr.request_reason AS requestReason,
                    rr.request_reason AS request_reason,
                    rr.status AS status,
                    DATE_FORMAT(rr.requested_at, '%Y-%m-%d') AS requested_date,
                    DATE_FORMAT(rr.requested_at, '%Y-%m-%d') AS requestedDate
                FROM refund_requests rr
                JOIN order_items oi ON rr.order_item_id = oi.order_item_id
                JOIN orders o ON oi.order_id = o.order_id
                JOIN customers c ON o.customer_id = c.customer_id
                WHERE (start_date IS NULL OR DATE(rr.requested_at) >= start_date)
                  AND (end_date IS NULL OR DATE(rr.requested_at) <= end_date)
                  AND (search_keyword IS NULL OR search_keyword = '' 
                       OR o.order_no LIKE CONCAT('%', search_keyword, '%')
                       OR c.full_name LIKE CONCAT('%', search_keyword, '%')
                       OR oi.product_name LIKE CONCAT('%', search_keyword, '%')
                       OR rr.request_reason LIKE CONCAT('%', search_keyword, '%'))
                  AND (status_filter IS NULL OR status_filter = '' OR UPPER(status_filter) = 'ALL' 
                       OR UPPER(rr.status) = UPPER(status_filter))
                ORDER BY rr.requested_at DESC;
            END
            """;
        jdbcTemplate.execute(sql);
        log.info("Initialized stored procedure: SP_GetRefundSummaryReport");
    }

    private void initLowStockAlertProcedure() {
        jdbcTemplate.execute("DROP PROCEDURE IF EXISTS `SP_GetLowStockAlertReport`");
        String sql = """
            CREATE PROCEDURE `SP_GetLowStockAlertReport`()
            BEGIN
                SELECT 
                    pv.sku AS sku,
                    p.product_name AS productName,
                    COALESCE(c.category_name, 'Uncategorized') AS categoryName,
                    i.quantity AS currentStock,
                    i.reserved_quantity AS reservedQuantity,
                    i.reorder_level AS reorderLevel,
                    i.status AS stockStatus
                FROM inventory i
                JOIN product_variants pv ON i.variant_id = pv.variant_id
                JOIN products p ON pv.product_id = p.product_id
                LEFT JOIN categories c ON p.category_id = c.category_id
                WHERE i.quantity <= i.reorder_level OR i.status IN ('LOW_STOCK', 'OUT_OF_STOCK')
                ORDER BY i.quantity ASC;
            END
            """;
        jdbcTemplate.execute(sql);
        log.info("Initialized stored procedure: SP_GetLowStockAlertReport");
    }

    private void initOrderFulfillmentProcedure() {
        jdbcTemplate.execute("DROP PROCEDURE IF EXISTS `SP_GetOrderFulfillmentReport`");
        String sql = """
            CREATE PROCEDURE `SP_GetOrderFulfillmentReport`(
                IN start_date DATE, 
                IN end_date DATE, 
                IN status_filter VARCHAR(50)
            )
            BEGIN
                SELECT 
                    o.order_id AS orderId,
                    DATE(o.created_at) AS orderDate,
                    COUNT(oi.order_item_id) AS totalItems,
                    o.total_amount AS totalAmount,
                    o.payment_status AS paymentStatus,
                    o.order_status AS orderStatus
                FROM orders o
                LEFT JOIN order_items oi ON o.order_id = oi.order_id
                WHERE DATE(o.created_at) BETWEEN start_date AND end_date
                  AND (status_filter IS NULL OR status_filter = '' OR status_filter = 'ALL' OR o.order_status = status_filter)
                GROUP BY o.order_id, DATE(o.created_at), o.total_amount, o.payment_status, o.order_status
                ORDER BY o.created_at DESC;
            END
            """;
        jdbcTemplate.execute(sql);
        log.info("Initialized stored procedure: SP_GetOrderFulfillmentReport");
    }

    private void initProductCatalogExportProcedure() {
        jdbcTemplate.execute("DROP PROCEDURE IF EXISTS `SP_GetProductCatalogExport`");
        String sql = """
            CREATE PROCEDURE `SP_GetProductCatalogExport`(
                IN search_keyword VARCHAR(100),
                IN category_id_filter BIGINT,
                IN brand_id_filter BIGINT,
                IN status_filter VARCHAR(50)
            )
            BEGIN
                SELECT 
                    p.product_name AS productName,
                    p.product_name AS product_name,
                    COALESCE(pv.sku, '-') AS sku,
                    COALESCE(pv.selling_price, 0.00) AS price,
                    IFNULL(i.quantity, 0) AS stock,
                    p.status AS status
                FROM products p
                LEFT JOIN product_variants pv ON p.product_id = pv.product_id
                LEFT JOIN categories c ON p.category_id = c.category_id
                LEFT JOIN brands b ON p.brand_id = b.brand_id
                LEFT JOIN inventory i ON pv.variant_id = i.variant_id
                WHERE (search_keyword IS NULL OR search_keyword = '' 
                       OR p.product_name LIKE CONCAT('%', search_keyword, '%') 
                       OR pv.sku LIKE CONCAT('%', search_keyword, '%')
                       OR b.brand_name LIKE CONCAT('%', search_keyword, '%'))
                  AND (category_id_filter IS NULL OR category_id_filter = 0 OR p.category_id = category_id_filter)
                  AND (brand_id_filter IS NULL OR brand_id_filter = 0 OR p.brand_id = brand_id_filter)
                  AND (status_filter IS NULL OR status_filter = '' OR UPPER(status_filter) = 'ALL' OR UPPER(p.status) = UPPER(status_filter))
                ORDER BY p.product_name ASC;
            END
            """;
        jdbcTemplate.execute(sql);
        log.info("Initialized stored procedure: SP_GetProductCatalogExport");
    }

    private void initBrandExportProcedure() {
        jdbcTemplate.execute("DROP PROCEDURE IF EXISTS `SP_GetBrandExportReport`");
        String sql = """
            CREATE PROCEDURE `SP_GetBrandExportReport`(
                IN search_keyword VARCHAR(100),
                IN status_filter VARCHAR(50)
            )
            BEGIN
                SELECT 
                    b.brand_name AS brandName,
                    b.brand_name AS brand_name,
                    IFNULL(
                        GROUP_CONCAT(DISTINCT COALESCE(c_direct.category_name, c_prod.category_name) ORDER BY COALESCE(c_direct.category_name, c_prod.category_name) SEPARATOR ', '), 
                        'No category'
                    ) AS categoryName,
                    IFNULL(
                        GROUP_CONCAT(DISTINCT COALESCE(c_direct.category_name, c_prod.category_name) ORDER BY COALESCE(c_direct.category_name, c_prod.category_name) SEPARATOR ', '), 
                        'No category'
                    ) AS category_name,
                    IFNULL(b.description, '-') AS description,
                    b.status AS status
                FROM brands b
                LEFT JOIN brand_categories bc ON b.brand_id = bc.brand_id
                LEFT JOIN categories c_direct ON bc.category_id = c_direct.category_id
                LEFT JOIN products p ON b.brand_id = p.brand_id
                LEFT JOIN categories c_prod ON p.category_id = c_prod.category_id
                WHERE (search_keyword IS NULL OR search_keyword = '' 
                       OR b.brand_name LIKE CONCAT('%', search_keyword, '%') 
                       OR b.description LIKE CONCAT('%', search_keyword, '%') 
                       OR c_direct.category_name LIKE CONCAT('%', search_keyword, '%')
                       OR c_prod.category_name LIKE CONCAT('%', search_keyword, '%'))
                  AND (status_filter IS NULL OR status_filter = '' OR status_filter = 'ALL' 
                       OR UPPER(b.status) = UPPER(status_filter))
                GROUP BY b.brand_id, b.brand_name, b.description, b.status
                ORDER BY b.brand_name ASC;
            END
            """;
        jdbcTemplate.execute(sql);
        log.info("Initialized stored procedure: SP_GetBrandExportReport");
    }

    private void initInventoryExportProcedure() {
        jdbcTemplate.execute("DROP PROCEDURE IF EXISTS `SP_GetInventoryExportReport`");
        String sql = """
            CREATE PROCEDURE `SP_GetInventoryExportReport`(
                IN tab_filter VARCHAR(50),
                IN search_keyword VARCHAR(100),
                IN status_filter VARCHAR(50)
            )
            BEGIN
                SELECT 
                    CONCAT(
                        pv.sku,
                        IFNULL((
                            SELECT CONCAT(' (', GROUP_CONCAT(vo.value ORDER BY vo.option_id SEPARATOR ' / '), ')')
                            FROM variant_option_values vov
                            JOIN variation_options vo ON vov.option_id = vo.option_id
                            WHERE vov.variant_id = pv.variant_id
                        ), '')
                    ) AS variant_and_sku,
                    CONCAT(
                        pv.sku,
                        IFNULL((
                            SELECT CONCAT(' (', GROUP_CONCAT(vo.value ORDER BY vo.option_id SEPARATOR ' / '), ')')
                            FROM variant_option_values vov
                            JOIN variation_options vo ON vov.option_id = vo.option_id
                            WHERE vov.variant_id = pv.variant_id
                        ), '')
                    ) AS variantAndSku,
                    p.product_name AS product,
                    pv.selling_price AS selling_price,
                    pv.selling_price AS sellingPrice,
                    IFNULL(i.quantity, 0) AS physical_stock,
                    IFNULL(i.quantity, 0) AS physicalStock,
                    IFNULL(i.reserved_quantity, 0) AS reserved_stock,
                    IFNULL(i.reserved_quantity, 0) AS reservedStock,
                    GREATEST(0, IFNULL(i.quantity, 0) - IFNULL(i.reserved_quantity, 0)) AS available_stock,
                    GREATEST(0, IFNULL(i.quantity, 0) - IFNULL(i.reserved_quantity, 0)) AS availableStock,
                    IFNULL(i.reorder_level, 0) AS reorder_level,
                    IFNULL(i.reorder_level, 0) AS reorderLevel,
                    CASE 
                        WHEN i.expiry_date IS NOT NULL AND i.batch_number IS NOT NULL AND TRIM(i.batch_number) != '' 
                            THEN CONCAT(DATE_FORMAT(i.expiry_date, '%Y-%m-%d'), ' (', i.batch_number, ')')
                        WHEN i.expiry_date IS NOT NULL 
                            THEN DATE_FORMAT(i.expiry_date, '%Y-%m-%d')
                        WHEN i.batch_number IS NOT NULL AND TRIM(i.batch_number) != '' 
                            THEN i.batch_number
                        ELSE '-'
                    END AS expiry_and_batch,
                    CASE 
                        WHEN i.expiry_date IS NOT NULL AND i.batch_number IS NOT NULL AND TRIM(i.batch_number) != '' 
                            THEN CONCAT(DATE_FORMAT(i.expiry_date, '%Y-%m-%d'), ' (', i.batch_number, ')')
                        WHEN i.expiry_date IS NOT NULL 
                            THEN DATE_FORMAT(i.expiry_date, '%Y-%m-%d')
                        WHEN i.batch_number IS NOT NULL AND TRIM(i.batch_number) != '' 
                            THEN i.batch_number
                        ELSE '-'
                    END AS expiryAndBatch,
                    IFNULL(i.status, 'NORMAL') AS status
                FROM inventory i
                INNER JOIN product_variants pv ON i.variant_id = pv.variant_id
                INNER JOIN products p ON pv.product_id = p.product_id
                WHERE (search_keyword IS NULL OR search_keyword = '' 
                       OR pv.sku LIKE CONCAT('%', search_keyword, '%') 
                       OR p.product_name LIKE CONCAT('%', search_keyword, '%'))
                  AND (status_filter IS NULL OR status_filter = '' OR UPPER(status_filter) = 'ALL' 
                       OR UPPER(i.status) = UPPER(status_filter))
                  AND (
                       tab_filter IS NULL OR tab_filter = '' OR UPPER(tab_filter) = 'ALL'
                       OR (UPPER(tab_filter) = 'LOW_STOCK' AND (i.quantity <= i.reorder_level OR UPPER(i.status) IN ('LOW_STOCK', 'OUT_OF_STOCK')))
                       OR (UPPER(tab_filter) = 'EXPIRING_SOON' AND i.expiry_date IS NOT NULL AND i.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY))
                  )
                ORDER BY p.product_name ASC, pv.sku ASC;
            END
            """;
        jdbcTemplate.execute(sql);
        log.info("Initialized stored procedure: SP_GetInventoryExportReport");
    }

    private void initOrdersPageExportProcedure() {
        jdbcTemplate.execute("DROP PROCEDURE IF EXISTS `SP_GetOrdersPageExportReport`");
        String sql = """
            CREATE PROCEDURE `SP_GetOrdersPageExportReport`(
                IN tab_status VARCHAR(50),
                IN search_keyword VARCHAR(100)
            )
            BEGIN
                SELECT 
                    o.order_no AS order_no,
                    o.order_no AS orderNo,
                    DATE_FORMAT(o.created_at, '%Y-%m-%d') AS order_date,
                    DATE_FORMAT(o.created_at, '%Y-%m-%d') AS orderDate,
                    IFNULL(c.full_name, 'Guest Customer') AS customer_name,
                    IFNULL(c.full_name, 'Guest Customer') AS customerName,
                    o.total_amount AS total_amount,
                    o.total_amount AS totalAmount,
                    o.order_status AS order_status,
                    o.order_status AS orderStatus,
                    o.payment_status AS payment_status,
                    o.payment_status AS paymentStatus
                FROM orders o
                LEFT JOIN customers c ON o.customer_id = c.customer_id
                WHERE (search_keyword IS NULL OR TRIM(search_keyword) = ''
                       OR o.order_no LIKE CONCAT('%', TRIM(search_keyword), '%')
                       OR c.full_name LIKE CONCAT('%', TRIM(search_keyword), '%'))
                  AND (
                       tab_status IS NULL OR TRIM(tab_status) = '' OR UPPER(TRIM(tab_status)) = 'ALL'
                       OR UPPER(o.order_status) = UPPER(TRIM(tab_status))
                       OR (UPPER(TRIM(tab_status)) = 'PAID' AND UPPER(o.payment_status) IN ('PAID', 'SUCCESS'))
                  )
                ORDER BY o.created_at DESC;
            END
            """;
        jdbcTemplate.execute(sql);
        log.info("Initialized stored procedure: SP_GetOrdersPageExportReport");
    }
}
