#!/bin/bash

# Test Data Analytics Module

BASE_URL="http://localhost:8080"

echo "=================================="
echo "Testing Data Analytics Module"
echo "=================================="

# Test health endpoint
echo -e "\n1. Health Check..."
curl -s -X GET "$BASE_URL/api/analytics/health" | jq '.'

# Test single image analysis
echo -e "\n2. Testing Single Image Analysis..."
curl -s -X POST "$BASE_URL/api/analytics/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "imagePath": "/path/to/test/image.jpg"
  }' | jq '.'

# Test batch processing
echo -e "\n3. Testing Batch Processing..."
curl -s -X POST "$BASE_URL/api/analytics/batch" \
  -H "Content-Type: application/json" \
  -d '{
    "imagePaths": [
      "/path/to/image1.jpg",
      "/path/to/image2.jpg",
      "/path/to/image3.jpg"
    ]
  }' | jq '.'

echo -e "\n=================================="
echo "Data Analytics Tests Complete"
echo "=================================="
