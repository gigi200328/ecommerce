-- =============================================================================
-- REFUND MANAGEMENT STORED PROCEDURE & SAMPLE SEED DATA
-- =============================================================================

-- 1. SP_GetRefundSummaryReport (Supports date range, search keyword, and status filter)
DROP PROCEDURE IF EXISTS `SP_GetRefundSummaryReport`;
DELIMITER //
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
END //
DELIMITER ;

-- =============================================================================
-- 2. Sample Seed Data for Testing Refund Requests, Orders, and Customers
-- =============================================================================

-- (A) Customers
INSERT INTO customers (customer_id, full_name, email, phone, password_hash, status, failed_login_attempts, created_at)
VALUES 
(1, 'Aung Kyaw Thu', 'aungkyawthu@gmail.com', '09971234567', '$2a$10$wK1Rk2eQ0wZ8H7X5G.YJkeWj8F2P3T3R4V6B8N0M2L4K6J8H0F2D', 'ACTIVE', 0, NOW() - INTERVAL 30 DAY),
(2, 'Su Myat Noe', 'sumyatnoe@gmail.com', '09781234568', '$2a$10$wK1Rk2eQ0wZ8H7X5G.YJkeWj8F2P3T3R4V6B8N0M2L4K6J8H0F2D', 'ACTIVE', 0, NOW() - INTERVAL 25 DAY),
(3, 'Zaw Lin Htet', 'zawlinhtet@gmail.com', '09451234569', '$2a$10$wK1Rk2eQ0wZ8H7X5G.YJkeWj8F2P3T3R4V6B8N0M2L4K6J8H0F2D', 'ACTIVE', 0, NOW() - INTERVAL 20 DAY),
(4, 'Thiri Shwe Sin', 'thirishwesin@gmail.com', '09261234570', '$2a$10$wK1Rk2eQ0wZ8H7X5G.YJkeWj8F2P3T3R4V6B8N0M2L4K6J8H0F2D', 'ACTIVE', 0, NOW() - INTERVAL 15 DAY),
(5, 'Min Khant Ko', 'minkhantko@gmail.com', '09961234571', '$2a$10$wK1Rk2eQ0wZ8H7X5G.YJkeWj8F2P3T3R4V6B8N0M2L4K6J8H0F2D', 'ACTIVE', 0, NOW() - INTERVAL 10 DAY)
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), email=VALUES(email);

-- (B) Orders
INSERT INTO orders (order_id, order_no, customer_id, subtotal_amount, discount_amount, tax_amount, shipping_fee, total_amount, order_status, payment_status, created_at)
VALUES 
(1, 'ORD-20261015-001', 1, 1199.00, 0.00, 0.00, 0.00, 1199.00, 'DELIVERED', 'SUCCESS', NOW() - INTERVAL 5 DAY),
(2, 'ORD-20261016-002', 2, 2499.00, 0.00, 0.00, 0.00, 2499.00, 'DELIVERED', 'SUCCESS', NOW() - INTERVAL 4 DAY),
(3, 'ORD-20261017-003', 3, 249.00, 0.00, 0.00, 0.00, 249.00, 'DELIVERED', 'REFUNDED', NOW() - INTERVAL 6 DAY),
(4, 'ORD-20261018-004', 4, 1448.00, 0.00, 0.00, 0.00, 1448.00, 'DELIVERED', 'PARTIALLY_REFUNDED', NOW() - INTERVAL 7 DAY),
(5, 'ORD-20261019-005', 5, 2499.00, 0.00, 0.00, 0.00, 2499.00, 'DELIVERED', 'SUCCESS', NOW() - INTERVAL 2 DAY),
(6, 'ORD-20261020-006', 1, 249.00, 0.00, 0.00, 0.00, 249.00, 'DELIVERED', 'SUCCESS', NOW() - INTERVAL 8 DAY)
ON DUPLICATE KEY UPDATE order_no=VALUES(order_no), payment_status=VALUES(payment_status);

-- (C) Order Items
INSERT INTO order_items (order_item_id, order_id, variant_id, product_name, qty, unit_price, subtotal, discount_amount)
VALUES 
(1, 1, 1, 'iPhone 15 Pro Max', 1, 1199.00, 1199.00, 0.00),
(2, 2, 2, 'MacBook Pro 16', 1, 2499.00, 2499.00, 0.00),
(3, 3, 3, 'AirPods Pro 2', 1, 249.00, 249.00, 0.00),
(4, 4, 1, 'iPhone 15 Pro Max', 1, 1199.00, 1199.00, 0.00),
(5, 4, 3, 'AirPods Pro 2', 1, 249.00, 249.00, 0.00),
(6, 5, 2, 'MacBook Pro 16', 1, 2499.00, 2499.00, 0.00),
(7, 6, 3, 'AirPods Pro 2', 1, 249.00, 249.00, 0.00)
ON DUPLICATE KEY UPDATE product_name=VALUES(product_name), unit_price=VALUES(unit_price);

-- (D) Refund Requests (Testing PENDING, APPROVED, REJECTED)
INSERT INTO refund_requests (refund_id, order_item_id, refund_quantity, request_reason, refund_amount, status, requested_at, processed_by, processed_at, remark)
VALUES 
(101, 1, 1, 'DEFECTIVE_PRODUCT', 1199.00, 'PENDING', NOW() - INTERVAL 1 DAY, NULL, NULL, NULL),
(102, 2, 1, 'DAMAGED_ITEM', 2499.00, 'PENDING', NOW() - INTERVAL 2 DAY, NULL, NULL, NULL),
(103, 6, 1, 'WRONG_ITEM_SENT', 2499.00, 'PENDING', NOW() - INTERVAL 3 HOUR, NULL, NULL, NULL),
(104, 3, 1, 'MISSING_PARTS', 249.00, 'APPROVED', NOW() - INTERVAL 5 DAY, 1, NOW() - INTERVAL 3 DAY, 'Approved: Returned original box with missing charging cable. Refund issued to customer.'),
(105, 5, 1, 'ORDERED_BY_MISTAKE', 249.00, 'APPROVED', NOW() - INTERVAL 6 DAY, 1, NOW() - INTERVAL 4 DAY, 'Approved: Unopened packaging received and verified.'),
(106, 4, 1, 'OTHER', 1199.00, 'REJECTED', NOW() - INTERVAL 4 DAY, 1, NOW() - INTERVAL 2 DAY, 'Rejected: Return period expired (> 14 days) and physical modification detected.'),
(107, 7, 1, 'DEFECTIVE_PRODUCT', 249.00, 'REJECTED', NOW() - INTERVAL 7 DAY, 1, NOW() - INTERVAL 5 DAY, 'Rejected: Quality assessment confirmed normal functionality; no hardware defect found.')
ON DUPLICATE KEY UPDATE status=VALUES(status), remark=VALUES(remark), refund_amount=VALUES(refund_amount);

-- (E) Admin Notifications
INSERT INTO admin_notifications (notification_id, title, message, type, reference_id, link, is_read, created_at)
VALUES 
(101, 'New Refund Request: ORD-20261015-001', 'Aung Kyaw Thu requested a refund of $1,199.00 for iPhone 15 Pro Max. Reason: DEFECTIVE_PRODUCT', 'REFUND_REQUEST', 101, '/admin/refunds', 0, NOW() - INTERVAL 1 DAY),
(102, 'New Refund Request: ORD-20261016-002', 'Su Myat Noe requested a refund of $2,499.00 for MacBook Pro 16. Reason: DAMAGED_ITEM', 'REFUND_REQUEST', 102, '/admin/refunds', 0, NOW() - INTERVAL 2 DAY),
(103, 'New Refund Request: ORD-20261019-005', 'Min Khant Ko requested a refund of $2,499.00 for MacBook Pro 16. Reason: WRONG_ITEM_SENT', 'REFUND_REQUEST', 103, '/admin/refunds', 0, NOW() - INTERVAL 3 HOUR),
(104, 'Refund Approved: ORD-20261017-003', 'Zaw Lin Htet refund of $249.00 for AirPods Pro 2 was APPROVED.', 'REFUND_REQUEST', 104, '/admin/refunds', 1, NOW() - INTERVAL 3 DAY)
ON DUPLICATE KEY UPDATE title=VALUES(title), message=VALUES(message), is_read=VALUES(is_read);

-- 9. Dynamic Stock Management Export Report (PDF & Excel)
DROP PROCEDURE IF EXISTS SP_GetInventoryExportReport;
DELIMITER //
CREATE PROCEDURE SP_GetInventoryExportReport(
    IN tab_filter VARCHAR(50),      -- 'ALL', 'LOW_STOCK', 'EXPIRING_SOON'
    IN search_keyword VARCHAR(100), -- Search by SKU or Product
    IN status_filter VARCHAR(50)    -- 'ALL', 'NORMAL', 'LOW_STOCK', 'OUT_OF_STOCK'
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
END //
DELIMITER ;

-- 10. Dynamic All Orders Page Export Report (Supports Status Tabs & Search)
DROP PROCEDURE IF EXISTS `SP_GetOrdersPageExportReport`;
DELIMITER //
CREATE PROCEDURE `SP_GetOrdersPageExportReport`(
    IN tab_status VARCHAR(50),      -- 'ALL', 'PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'
    IN search_keyword VARCHAR(100)  -- Search by Order No or Customer Name
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
END //
DELIMITER ;
