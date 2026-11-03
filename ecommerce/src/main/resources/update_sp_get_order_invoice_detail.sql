-- =========================================================================
-- Update Stored Procedure: sp_get_order_invoice_detail
-- Purpose: 
--   1. Fix column name for customers: c.phone AS customer_phone
--      (customers table column is `phone`, NOT `phone_number`)
--   2. Remove oi.discount_amount from Result Set 2 to match
--      the updated order_items table schema.
-- =========================================================================

USE ecom;

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

    -- 3. Result Set 2: Order Line Items (Removed oi.discount_amount)
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
