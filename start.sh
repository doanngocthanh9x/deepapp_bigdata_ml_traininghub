#!/bin/bash

# DeepApp Auto-Runner
# Tự động chạy container với cấu hình tối ưu

set -e

IMAGE_NAME="${IMAGE_NAME:-deepapp-bigdata-ml-traininghub:latest}"
CONTAINER_NAME="${CONTAINER_NAME:-deepapp-container}"

# Detect current directory
WORK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 DeepApp Auto-Deploy"
echo "📍 Working Directory: $WORK_DIR"
echo "📦 Image: $IMAGE_NAME"
echo ""

# Stop existing container
echo "🛑 Stopping existing container..."
docker stop $CONTAINER_NAME 2>/dev/null || true
docker rm $CONTAINER_NAME 2>/dev/null || true

# Create directories
echo "📁 Creating directories..."
mkdir -p "$WORK_DIR/logs"
mkdir -p "$WORK_DIR/src/main/resources/models" 2>/dev/null || true

# Run container with auto-detected paths
echo "🐳 Starting container..."
docker run -d \
  --name $CONTAINER_NAME \
  -p 8080:8080 \
  -p 50051:50051 \
  -v "$WORK_DIR/logs:/app/logs" \
  -v "$WORK_DIR/src/main/resources/models:/app/config/src/main/resources/models:ro" \
  --restart unless-stopped \
  $IMAGE_NAME

echo ""
echo "✅ Container started successfully!"
echo ""
echo "🌐 Access URLs:"
echo "   Web UI: http://localhost:8080"
echo "   Health: http://localhost:8080/actuator/health"
echo "   gRPC:  localhost:50051"
echo ""
echo "📊 Commands:"
echo "   docker logs -f $CONTAINER_NAME    # View logs"
echo "   docker exec -it $CONTAINER_NAME bash  # Shell access"
echo "   docker stop $CONTAINER_NAME        # Stop container"