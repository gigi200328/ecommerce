-- =========================================================================
-- Fix & Update All Order Stored Procedures
-- 1. Fix customers column: c.phone AS customer_phone (table has `phone`, not `phone_number`)
-- 2. Remove oi.discount_amount from sp_get_order_invoice_detail (order_items table does not have discount_amount)
-- =========================================================================

USE ecom;

-- -------------------------------------------------------------------------
-- 1. sp_get_order_invoice_detail
-- -------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_get_order_invoice_detail;

DELIMITER $$

CREATE PROCEDURE sp_get_order_invoice_detail(
    IN p_order_id BIGINT,
    IN p_customer_id BIGINT
)
BEGIN
    DECLARE v_owner_id BIGINT;

    -- 1. Check if the order exists and verify ownership (IDOR guard)
    SELECT customer_id INTO v_owner_id 
    FROM orders 
    WHERE order_id = p_order_id;

    IF v_owner_id IS NULL THEN
        SIGNAL SQLSTATE '45000' 
            SET MESSAGE_TEXT = 'Order not found.';
    END IF;

    IF v_owner_id <> p_customer_id THEN
        SIGNAL SQLSTATE '45000' 
            SET MESSAGE_TEXT = 'Access Denied: Order does not belong to this customer.';
    END IF;

    -- 2. Result Set 1: Order Header + Customer + Shipping Address + Payment + Shipment
    SELECT 
        o.order_id,
        o.order_no,
        o.order_status,
        o.payment_status,
        o.created_at AS order_placed_at,
        o.modified_at AS order_modified_at,
        o.subtotal_amount,
        o.shipping_fee,
        o.tax_amount,
        o.discount_amount,
        o.total_amount,
        c.customer_id,
        c.full_name AS customer_name,
        c.email AS customer_email,
        c.phone AS customer_phone,
        oa.recipient_name,
        oa.phone_number AS recipient_phone,
        oa.address_line1,
        oa.address_line2,
        oa.township,
        oa.city,
        oa.region_or_state,
        CONCAT_WS(', ', oa.address_line1, oa.address_line2, oa.township, oa.city, oa.region_or_state) AS full_address_text,
        p.payment_id,
        p.payment_method,
        p.payment_status AS latest_payment_status,
        p.transaction_ref,
        p.paid_at,
        p.amount AS latest_payment_amount,
        s.shipment_id,
        s.courier_name,
        s.tracking_number,
        s.shipment_status,
        s.shipped_at,
        s.delivered_at
    FROM orders o
    JOIN customers c ON o.customer_id = c.customer_id
    LEFT JOIN order_addresses oa ON o.order_id = oa.order_id
    LEFT JOIN (
        SELECT * FROM payments 
        WHERE order_id = p_order_id 
        ORDER BY created_at DESC 
        LIMIT 1
    ) p ON p.order_id = o.order_id
    LEFT JOIN (
        SELECT * FROM shipments 
        WHERE order_id = p_order_id 
        ORDER BY created_at DESC 
        LIMIT 1
    ) s ON s.order_id = o.order_id
    WHERE o.order_id = p_order_id;

    -- 3. Result Set 2: Order Line Items (oi.discount_amount removed)
    SELECT 
        oi.order_item_id,
        oi.product_name,
        pv.sku,
        oi.variant_attributes,
        oi.unit_price,
        oi.qty,
        oi.subtotal
    FROM order_items oi
    LEFT JOIN product_variants pv ON oi.variant_id = pv.variant_id
    WHERE oi.order_id = p_order_id;

END$$

DELIMITER ;

-- -------------------------------------------------------------------------
-- 2. sp_get_customer_order_history_report
-- -------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_get_customer_order_history_report;

DELIMITER $$

CREATE PROCEDURE sp_get_customer_order_history_report(
    IN p_customer_id BIGINT,
    IN p_start_date DATE,
    IN p_end_date DATE,
    IN p_order_status VARCHAR(50),
    IN p_order_no VARCHAR(50)
)
BEGIN
    SELECT 
        o.order_id,
        o.order_no,
        o.created_at AS order_date,
        o.order_status,
        o.payment_status,
        (SELECT p.payment_method FROM payments p WHERE p.order_id = o.order_id ORDER BY p.created_at DESC LIMIT 1) AS payment_method,
        (SELECT GROUP_CONCAT(CONCAT(oi.product_name, ' (x', oi.qty, ')') SEPARATOR ', ') FROM order_items oi WHERE oi.order_id = o.order_id) AS items_summary,
        (SELECT COALESCE(SUM(oi.qty), 0) FROM order_items oi WHERE oi.order_id = o.order_id) AS total_item_quantity,
        o.subtotal_amount,
        o.shipping_fee,
        o.discount_amount,
        o.total_amount,
        c.full_name AS customer_name,
        c.email AS customer_email,
        c.phone AS customer_phone
    FROM orders o
    JOIN customers c ON o.customer_id = c.customer_id
    WHERE o.customer_id = p_customer_id
      AND (p_start_date IS NULL OR DATE(o.created_at) >= p_start_date)
      AND (p_end_date IS NULL OR DATE(o.created_at) <= p_end_date)
      AND (p_order_status IS NULL OR p_order_status = '' OR o.order_status = p_order_status)
      AND (p_order_no IS NULL OR p_order_no = '' OR o.order_no LIKE CONCAT('%', p_order_no, '%'))
    ORDER BY o.created_at DESC, o.order_id DESC;
END$$

DELIMITER ;
