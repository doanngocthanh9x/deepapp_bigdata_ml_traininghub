#!/bin/bash

# Test Document Processing Module

BASE_URL="http://localhost:8080"

echo "=================================="
echo "Testing Document Processing Module"
echo "=================================="

# Test health endpoint
echo -e "\n1. Health Check..."
curl -s -X GET "$BASE_URL/api/documents/health" | jq '.'

# Test OCR only
echo -e "\n2. Testing OCR..."
curl -s -X POST "$BASE_URL/api/documents/ocr" \
  -H "Content-Type: application/json" \
  -d '{
    "imagePath": "/path/to/test/image.jpg"
  }' | jq '.'

# Test NER only
echo -e "\n3. Testing NER..."
curl -s -X POST "$BASE_URL/api/documents/ner" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "John Doe works at Google in California. He was born in 1990."
  }' | jq '.'

# Test full document processing
echo -e "\n4. Testing Full Document Processing (OCR + NER)..."
curl -s -X POST "$BASE_URL/api/documents/process" \
  -H "Content-Type: application/json" \
  -d '{
    "imagePath": "/path/to/test/document.jpg"
  }' | jq '.'

# Test with language
echo -e "\n5. Testing Document Processing with Language..."
curl -s -X POST "$BASE_URL/api/documents/process/language" \
  -H "Content-Type: application/json" \
  -d '{
    "imagePath": "/path/to/test/document.jpg",
    "language": "vie"
  }' | jq '.'

echo -e "\n=================================="
echo "Document Processing Tests Complete"
echo "=================================="
