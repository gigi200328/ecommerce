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
-- 2. Sample Seed Data for refund_requests
-- (Uses existing order_items in database, or generates sample refund rows)
-- =============================================================================

INSERT INTO refund_requests (refund_id, order_item_id, refund_quantity, request_reason, refund_amount, status, requested_at, remark)
SELECT 101, MIN(oi.order_item_id), 1, 'DAMAGED_ITEM', 45.00, 'PENDING', NOW() - INTERVAL 2 DAY, NULL
FROM order_items oi
WHERE EXISTS (SELECT 1 FROM order_items)
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO refund_requests (refund_id, order_item_id, refund_quantity, request_reason, refund_amount, status, requested_at, remark)
SELECT 102, MAX(oi.order_item_id), 2, 'DEFECTIVE_PRODUCT', 120.00, 'PENDING', NOW() - INTERVAL 1 DAY, NULL
FROM order_items oi
WHERE EXISTS (SELECT 1 FROM order_items)
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO refund_requests (refund_id, order_item_id, refund_quantity, request_reason, refund_amount, status, requested_at, remark)
SELECT 103, MIN(oi.order_item_id), 1, 'WRONG_ITEM_SENT', 89.50, 'APPROVED', NOW() - INTERVAL 5 DAY, 'Approved refund. Item returned to warehouse.'
FROM order_items oi
WHERE EXISTS (SELECT 1 FROM order_items)
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO refund_requests (refund_id, order_item_id, refund_quantity, request_reason, refund_amount, status, requested_at, remark)
SELECT 104, MAX(oi.order_item_id), 1, 'ORDERED_BY_MISTAKE', 250.00, 'APPROVED', NOW() - INTERVAL 7 DAY, 'Approved prior to delivery dispatch.'
FROM order_items oi
WHERE EXISTS (SELECT 1 FROM order_items)
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO refund_requests (refund_id, order_item_id, refund_quantity, request_reason, refund_amount, status, requested_at, remark)
SELECT 105, MIN(oi.order_item_id), 1, 'MISSING_PARTS', 65.00, 'REJECTED', NOW() - INTERVAL 4 DAY, 'Packaging inspection showed all components were intact.'
FROM order_items oi
WHERE EXISTS (SELECT 1 FROM order_items)
ON DUPLICATE KEY UPDATE status = VALUES(status);
