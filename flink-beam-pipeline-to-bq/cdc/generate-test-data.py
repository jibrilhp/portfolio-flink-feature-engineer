#!/usr/bin/env python3
import psycopg2
import random
import time
from datetime import datetime

def generate_test_data():
    """Generate test data for CDC testing"""
    
    conn = psycopg2.connect(
        host='localhost',
        database='sourcedb',
        user='postgres',
        password='postgres'
    )
    
    cursor = conn.cursor()
    
    # Insert new users
    users = [
        ('Alice Brown', 'alice@example.com'),
        ('Charlie Wilson', 'charlie@example.com'),
        ('Diana Prince', 'diana@example.com'),
    ]
    
    for name, email in users:
        try:
            cursor.execute(
                "INSERT INTO users (name, email) VALUES (%s, %s)",
                (name, email)
            )
            print(f"✅ Inserted user: {name}")
        except Exception as e:
            print(f"❌ Error inserting user {name}: {e}")
    
    # Update existing users
    cursor.execute("UPDATE users SET updated_at = NOW() WHERE id = 1")
    print("✅ Updated user with ID 1")
    
    # Insert new orders
    cursor.execute(
        "INSERT INTO orders (user_id, product_name, quantity, price) VALUES (%s, %s, %s, %s)",
        (1, 'Monitor', 1, 299.99)
    )
    print("✅ Inserted new order")
    
    # Update order status
    cursor.execute(
        "UPDATE orders SET status = 'completed' WHERE id = 1"
    )
    print("✅ Updated order status")
    
    conn.commit()
    cursor.close()
    conn.close()
    
    print(f"🎉 Test data generated at {datetime.now()}")

if __name__ == "__main__":
    generate_test_data()
