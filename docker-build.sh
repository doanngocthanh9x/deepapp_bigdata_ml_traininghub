#!/bin/bash

set -e

echo "=========================================="
echo "Building DeepApp Docker Image"
echo "=========================================="

# Change to project directory
cd "$(dirname "$0")"

# Step 1: Build Java application locally
echo "Step 1: Building Java application..."
mvn clean package -DskipTests

if [ ! -f "target/deepapp_main-0.0.1-SNAPSHOT.jar" ]; then
    echo "ERROR: Java build failed - JAR file not found"
    exit 1
fi
echo "✓ Java application built successfully"

# Clean old containers
echo ""
echo "Step 2: Cleaning up old containers..."
docker-compose down -v 2>/dev/null || true

# Build Docker image
echo ""
echo "Step 3: Building Docker image..."
docker-compose build --no-cache

echo ""
echo "=========================================="
echo "Build Complete!"
echo "=========================================="
echo ""
echo "To start the application, run:"
echo "  ./docker-run.sh"
echo ""
