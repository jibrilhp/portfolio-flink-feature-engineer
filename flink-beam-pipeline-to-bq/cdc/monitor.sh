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
