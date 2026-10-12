import mysql.connector

try:
    conn = mysql.connector.connect(
        host='localhost',
        port=3306,
        user='root',
        password='shwe123',
        database='ecommerce'
    )
    cursor = conn.cursor()
    conn.autocommit = True

    queries = [
        "INSERT INTO role (role_name) VALUES ('ADMIN') ON DUPLICATE KEY UPDATE role_name='ADMIN'",
        "INSERT INTO users (user_id, user_name, email, password_hash, user_role, status, created_at) VALUES (1, 'admin', 'admin@shop.com', 'hashed_pass', 1, 'ACTIVE', NOW()) ON DUPLICATE KEY UPDATE status='ACTIVE'",
        "INSERT INTO customers (customer_id, full_name, email, password_hash, created_at) VALUES (1, 'Test Customer', 'test@example.com', 'hashed_pass', NOW()) ON DUPLICATE KEY UPDATE full_name='Test Customer'",
        "INSERT INTO categories (category_id, category_name, created_at, created_by) VALUES (1, 'Electronics', NOW(), 1) ON DUPLICATE KEY UPDATE category_name='Electronics'",
        "INSERT INTO categories (category_id, category_name, created_at, created_by) VALUES (2, 'Clothing', NOW(), 1) ON DUPLICATE KEY UPDATE category_name='Clothing'",
        "INSERT INTO products (product_id, category_id, product_name, status, created_at, created_by) VALUES (1, 1, 'iPhone 15', 'ACTIVE', NOW(), 1) ON DUPLICATE KEY UPDATE product_name='iPhone 15'",
        "INSERT INTO products (product_id, category_id, product_name, status, created_at, created_by) VALUES (2, 1, 'Samsung Galaxy S23', 'ACTIVE', NOW(), 1) ON DUPLICATE KEY UPDATE product_name='Samsung Galaxy S23'",
        "INSERT INTO products (product_id, category_id, product_name, status, created_at, created_by) VALUES (3, 2, 'Nike Air Max', 'ACTIVE', NOW(), 1) ON DUPLICATE KEY UPDATE product_name='Nike Air Max'",
        "INSERT INTO product_variants (variant_id, product_id, sku, selling_price, status) VALUES (1, 1, 'IPH15-01', 999.00, 'ACTIVE') ON DUPLICATE KEY UPDATE sku='IPH15-01'",
        "INSERT INTO product_variants (variant_id, product_id, sku, selling_price, status) VALUES (2, 2, 'SAMS23-01', 899.00, 'ACTIVE') ON DUPLICATE KEY UPDATE sku='SAMS23-01'",
        "INSERT INTO product_variants (variant_id, product_id, sku, selling_price, status) VALUES (3, 3, 'NIKE-01', 120.00, 'ACTIVE') ON DUPLICATE KEY UPDATE sku='NIKE-01'",
        "INSERT INTO orders (order_id, order_no, customer_id, subtotal_amount, discount_amount, tax_amount, shipping_fee, order_status, payment_status, total_amount, created_at) VALUES (1, 'ORD-20261001', 1, 1998.00, 0, 0, 0, 'DELIVERED', 'SUCCESS', 1998.00, '2026-10-01 10:00:00') ON DUPLICATE KEY UPDATE order_no='ORD-20261001'",
        "INSERT INTO orders (order_id, order_no, customer_id, subtotal_amount, discount_amount, tax_amount, shipping_fee, order_status, payment_status, total_amount, created_at) VALUES (2, 'ORD-20261002', 1, 899.00, 0, 0, 0, 'DELIVERED', 'SUCCESS', 899.00, '2026-10-02 11:00:00') ON DUPLICATE KEY UPDATE order_no='ORD-20261002'",
        "INSERT INTO orders (order_id, order_no, customer_id, subtotal_amount, discount_amount, tax_amount, shipping_fee, order_status, payment_status, total_amount, created_at) VALUES (3, 'ORD-20261003', 1, 120.00, 0, 0, 0, 'DELIVERED', 'SUCCESS', 120.00, '2026-10-03 12:00:00') ON DUPLICATE KEY UPDATE order_no='ORD-20261003'",
        "INSERT INTO order_items (order_item_id, order_id, variant_id, qty, unit_price, subtotal, discount_amount, product_name) VALUES (1, 1, 1, 2, 999.00, 1998.00, 0, 'iPhone 15') ON DUPLICATE KEY UPDATE qty=2",
        "INSERT INTO order_items (order_item_id, order_id, variant_id, qty, unit_price, subtotal, discount_amount, product_name) VALUES (2, 2, 2, 1, 899.00, 899.00, 0, 'Samsung Galaxy S23') ON DUPLICATE KEY UPDATE qty=1",
        "INSERT INTO order_items (order_item_id, order_id, variant_id, qty, unit_price, subtotal, discount_amount, product_name) VALUES (3, 3, 3, 1, 120.00, 120.00, 0, 'Nike Air Max') ON DUPLICATE KEY UPDATE qty=1"
    ]

    for q in queries:
        try:
            print(f"Executing: {q[:40]}...")
            cursor.execute(q)
        except Exception as e:
            print(f"Failed query: {q[:40]} - Error: {e}")
            break

    print("Done!")

except Exception as e:
    print(f"Error: {e}")
