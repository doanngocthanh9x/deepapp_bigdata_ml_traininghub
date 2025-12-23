#!/bin/bash

set -e

echo "=========================================="
echo "Starting DeepApp Containers"
echo "=========================================="

# Change to project directory
cd "$(dirname "$0")"

# Start containers
docker-compose up -d

echo ""
echo "Waiting for application to be healthy..."
sleep 10

# Show logs
echo ""
echo "=========================================="
echo "Container Status"
echo "=========================================="
docker-compose ps

echo ""
echo "=========================================="
echo "Application Logs (last 20 lines)"
echo "=========================================="
docker-compose logs --tail=20

echo ""
echo "=========================================="
echo "DeepApp is running!"
echo "=========================================="
echo "  API: http://localhost:8080"
echo "  Health: http://localhost:8080/actuator/health"
echo ""
echo "To view logs:"
echo "  docker-compose logs -f"
echo ""
echo "To stop:"
echo "  docker-compose down"
echo ""
