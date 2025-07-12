#!/bin/bash

# setup.sh - Setup script for CDC Pipeline POC

set -e

echo "🚀 Setting up CDC Pipeline POC..."

# Create required directories
mkdir -p init-scripts
mkdir -p connectors
mkdir -p flink-jobs
mkdir -p flink-lib
mkdir -p gcp-credentials

# Download required JAR files for Flink
echo "📦 Downloading required JAR files..."

# Flink Kafka Connector
curl -L -o flink-lib/flink-connector-kafka-1.18.0.jar \
  https://repo1.maven.org/maven2/org/apache/flink/flink-connector-kafka/1.18.0/flink-connector-kafka-1.18.0.jar

# Flink JSON Format
curl -L -o flink-lib/flink-json-1.18.0.jar \
  https://repo1.maven.org/maven2/org/apache/flink/flink-json/1.18.0/flink-json-1.18.0.jar

# BigQuery Connector for Flink (you'll need to build this or use alternative)
# Note: This is a placeholder - you'll need to add the actual BigQuery connector JAR
echo "⚠️  Note: You'll need to add the BigQuery connector JAR to flink-lib/"

# Create BigQuery dataset and table setup script
cat > setup-bigquery.sql << 'EOF'
-- Run this in BigQuery console to create the dataset and table

CREATE SCHEMA IF NOT EXISTS `your-project-id.cdc_dataset`;

CREATE TABLE IF NOT EXISTS `your-project-id.cdc_dataset.cdc_events` (
  table_name STRING,
  operation STRING,
  record_id STRING,
  data STRING,
  timestamp_ms INT64,
  processing_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
);
EOF

# Create connector deployment script
cat > deploy-connector.sh << 'EOF'
#!/bin/bash

# Wait for Kafka Connect to be ready
echo "⏳ Waiting for Kafka Connect to be ready..."
until curl -f -s http://localhost:8083/connectors; do
  sleep 5
done

# Deploy Debezium PostgreSQL connector
echo "📡 Deploying Debezium PostgreSQL connector..."
curl -X POST -H "Content-Type: application/json" \
  --data @debezium-postgres-connector.json \
  http://localhost:8083/connectors

echo "✅ Connector deployed successfully!"

# Check connector status
echo "🔍 Checking connector status..."
curl -s http://localhost:8083/connectors/postgres-connector/status | jq .
EOF

chmod +x deploy-connector.sh

# Create monitoring script
cat > monitor.sh << 'EOF'
#!/bin/bash

echo "📊 CDC Pipeline Monitoring Dashboard"
echo "=================================="
echo
echo "🌐 Web UIs:"
echo "  - Kafka UI: http://localhost:8080"
echo "  - Flink Dashboard: http://localhost:8081"
echo "  - Kafka Connect: http://localhost:8083"
echo
echo "📡 Kafka Topics:"
kafka-topics --bootstrap-server localhost:9092 --list
echo
echo "🔗 Connectors:"
curl -s http://localhost:8083/connectors | jq .
echo
echo "📈 Topic Messages (last 10):"
echo "Users topic:"
kafka-console-consumer --bootstrap-server localhost:9092 --topic cdc.public.users --from-beginning --max-messages 10
EOF

chmod +x monitor.sh

# Create test data generator
cat > generate-test-data.py << 'EOF'
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
EOF

chmod +x generate-test-data.py

# Create cleanup script
cat > cleanup.sh << 'EOF'
#!/bin/bash

echo "🧹 Cleaning up CDC Pipeline POC..."

# Stop and remove containers
docker-compose down -v

# Remove downloaded files
rm -rf flink-lib/*.jar

echo "✅ Cleanup completed!"
EOF

chmod +x cleanup.sh

echo "✅ Setup completed!"
echo
echo "📋 Next Steps:"
echo "1. Add your GCP service account JSON to gcp-credentials/service-account.json"
echo "2. Update the BigQuery project ID in the Flink job"
echo "3. Run: docker-compose up -d"
echo "4. Wait for services to start, then run: ./deploy-connector.sh"
echo "5. Monitor with: ./monitor.sh"
echo "6. Generate test data with: ./generate-test-data.py"
echo
echo "🌐 Access URLs:"
echo "  - Kafka UI: http://localhost:8080"
echo "  - Flink Dashboard: http://localhost:8081"
echo "  - Kafka Connect: http://localhost:8083"