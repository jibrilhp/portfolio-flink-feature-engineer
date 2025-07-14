#!/usr/bin/env python3
import psycopg2
import random
import time
from datetime import datetime
from faker import Faker

# Initialize Faker for generating random data
fake = Faker()

def generate_test_data():
    """Generate dynamic test data for CDC testing"""
    try:
        # Connect to PostgreSQL
        conn = psycopg2.connect(
            host='localhost',
            database='sourcedb',
            user='postgres',
            password='postgres'
        )
        cursor = conn.cursor()

        while True:
            # Generate random data
            name = fake.name()
            email = fake.email()
            product_name = fake.word().capitalize() + " " + random.choice(['Laptop', 'Phone', 'Tablet', 'Monitor', 'Headphones'])
            quantity = random.randint(1, 5)
            price = round(random.uniform(10.99, 999.99), 2)
            user_id = random.randint(1, 10)  # Assuming users with IDs 1-10 exist
            status = random.choice(['pending', 'processing', 'completed', 'cancelled'])

            # Insert new user
            try:
                cursor.execute(
                    "INSERT INTO users (name, email) VALUES (%s, %s) ON CONFLICT DO NOTHING",
                    (name, email)
                )
                print(f"✅ Inserted user: {name} ({email})")
            except Exception as e:
                print(f"❌ Error inserting user {name}: {e}")

            # Update a random existing user
            try:
                cursor.execute(
                    "UPDATE users SET updated_at = NOW() WHERE id = %s",
                    (random.randint(1, 10),)  # Random user ID
                )
                print(f"✅ Updated user with ID {user_id}")
            except Exception as e:
                print(f"❌ Error updating user: {e}")

            # Insert new order
            try:
                cursor.execute(
                    "INSERT INTO orders (user_id, product_name, quantity, price) VALUES (%s, %s, %s, %s)",
                    (user_id, product_name, quantity, price)
                )
                print(f"✅ Inserted order: {product_name} (Qty: {quantity}, Price: {price})")
            except Exception as e:
                print(f"❌ Error inserting order {product_name}: {e}")

            # Update a random order status
            try:
                cursor.execute(
                    "UPDATE orders SET status = %s WHERE id = %s",
                    (status, random.randint(1, 10))  # Random order ID
                )
                print(f"✅ Updated order status to {status}")
            except Exception as e:
                print(f"❌ Error updating order status: {e}")

            # Commit changes
            conn.commit()

            # Random delay between 5 and 10 seconds
            delay = random.uniform(5, 10)
            print(f"⏳ Sleeping for {delay:.2f} seconds at {datetime.now()}")
            time.sleep(delay)

    except Exception as e:
        print(f"❌ Database connection error: {e}")
    finally:
        # Clean up
        cursor.close()
        conn.close()
        print("🔒 Database connection closed")

if __name__ == "__main__":
    generate_test_data()