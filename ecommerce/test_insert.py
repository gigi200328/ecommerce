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
    cursor.execute("INSERT INTO categories (category_id, category_name) VALUES (1, 'Electronics')")
    conn.commit()
    print("Inserted successfully!")
except Exception as e:
    print(f"Error: {e}")
