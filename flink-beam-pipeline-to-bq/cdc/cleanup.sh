#!/bin/bash

echo "🧹 Cleaning up CDC Pipeline POC..."

# Stop and remove containers
docker-compose down -v

# Remove downloaded files
rm -rf flink-lib/*.jar

echo "✅ Cleanup completed!"
