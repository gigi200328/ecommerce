-- =============================================================================
-- E-COMMERCE BACK-OFFICE REPORT STORED PROCEDURES
-- =============================================================================
-- Description: Contains the 6 Stored Procedures required for Back-Office Reports.
-- Usage: Execute this entire script in MySQL Workbench or your MySQL client.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. SP_GetDailyProductSales
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `SP_GetDailyProductSales`;
DELIMITER //
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
END //
DELIMITER ;

-- -----------------------------------------------------------------------------
-- 2. SP_GetCategorySummaryReport
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `SP_GetCategorySummaryReport`;
DELIMITER //
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
END //
DELIMITER ;

-- -----------------------------------------------------------------------------
-- 3. SP_GetTopSellingProducts
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `SP_GetTopSellingProducts`;
DELIMITER //
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
END //
DELIMITER ;

-- -----------------------------------------------------------------------------
-- 4. SP_GetRefundSummaryReport
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `SP_GetRefundSummaryReport`;
DELIMITER //
CREATE PROCEDURE `SP_GetRefundSummaryReport`(
    IN startDate DATE,
    IN endDate DATE
)
BEGIN
    SELECT 
        rr.refund_id AS refundId,
        oi.order_id AS orderId,
        oi.product_name AS productName,
        rr.refund_quantity AS refundQuantity,
        rr.refund_amount AS refundAmount,
        rr.request_reason AS requestReason,
        rr.status AS status,
        DATE(rr.requested_at) AS requestedDate
    FROM refund_requests rr
    JOIN order_items oi ON rr.order_item_id = oi.order_item_id
    WHERE DATE(rr.requested_at) BETWEEN startDate AND endDate
    ORDER BY rr.requested_at DESC;
END //
DELIMITER ;

-- -----------------------------------------------------------------------------
-- 5. SP_GetLowStockAlertReport
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `SP_GetLowStockAlertReport`;
DELIMITER //
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
END //
DELIMITER ;

-- -----------------------------------------------------------------------------
-- 6. SP_GetOrderFulfillmentReport
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `SP_GetOrderFulfillmentReport`;
DELIMITER //
CREATE PROCEDURE `SP_GetOrderFulfillmentReport`(
    IN start_date DATE, 
    IN end_date DATE, 
    IN status_filter VARCHAR(50)
)
BEGIN
    SELECT 
        o.order_id,
        DATE(o.created_at) AS order_date,
        COUNT(oi.order_item_id) AS total_items,
        o.total_amount,
        o.payment_status,
        o.order_status
    FROM orders o
    LEFT JOIN order_items oi ON o.order_id = oi.order_id
    WHERE DATE(o.created_at) BETWEEN start_date AND end_date
      AND (status_filter IS NULL OR status_filter = '' OR status_filter = 'ALL' OR o.order_status = status_filter)
    GROUP BY o.order_id, DATE(o.created_at), o.total_amount, o.payment_status, o.order_status
    ORDER BY o.created_at DESC;
END //
DELIMITER ;
