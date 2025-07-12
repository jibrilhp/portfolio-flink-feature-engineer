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
