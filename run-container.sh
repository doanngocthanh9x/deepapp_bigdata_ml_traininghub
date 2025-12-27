#!/bin/bash

# Script chạy DeepApp container với cấu hình mặc định
# Không cần truyền tham số gì thêm

IMAGE_NAME="${IMAGE_NAME:-deepapp-bigdata-ml-traininghub:latest}"
CONTAINER_NAME="${CONTAINER_NAME:-deepapp-container}"

# Auto-detect current directory for volumes
CURRENT_DIR="${CURRENT_DIR:-$(pwd)}"

echo "🚀 Khởi động DeepApp container..."
echo "📦 Image: $IMAGE_NAME"
echo "🏷️  Container: $CONTAINER_NAME"
echo "📁 Working dir: $CURRENT_DIR"
echo ""

# Stop existing container if running
docker stop $CONTAINER_NAME 2>/dev/null || true
docker rm $CONTAINER_NAME 2>/dev/null || true

# Run container with all default settings
docker run -d \
  --name $CONTAINER_NAME \
  -p 8080:8080 \
  -p 50051:50051 \
  -v "$CURRENT_DIR/logs:/app/logs" \
  -v "$CURRENT_DIR/src/main/resources/models:/app/config/src/main/resources/models:ro" \
  --restart unless-stopped \
  $IMAGE_NAME

echo "✅ Container '$CONTAINER_NAME' đã được khởi động!"
echo ""
echo "🌐 Truy cập: http://localhost:8080"
echo "📊 Health check: http://localhost:8080/actuator/health"
echo ""
echo "📋 Lệnh hữu ích:"
echo "   docker logs -f $CONTAINER_NAME    # Xem logs"
echo "   docker stop $CONTAINER_NAME       # Dừng container"
echo "   docker restart $CONTAINER_NAME    # Khởi động lại"