#!/bin/bash

# Test C++ Workers via Java API

BASE_URL="http://localhost:8080"

echo "=================================="
echo "Testing C++ Workers via Java"
echo "=================================="

# Test health
echo -e "\n1. Health Check..."
curl -s -X GET "$BASE_URL/api/cpp/health" | jq '.'

# Test AAA0_0100_W - echo
echo -e "\n2. Testing AAA0_0100_W - echo..."
curl -s -X POST "$BASE_URL/api/cpp/aaa0_0100" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "echo",
    "data": "Hello from Java!"
  }' | jq '.'

# Test AAA0_0100_W - process
echo -e "\n3. Testing AAA0_0100_W - process..."
curl -s -X POST "$BASE_URL/api/cpp/aaa0_0100" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "process",
    "data": "convert this to uppercase"
  }' | jq '.'

# Test AAA0_0100_W - transform
echo -e "\n4. Testing AAA0_0100_W - transform..."
curl -s -X POST "$BASE_URL/api/cpp/aaa0_0100" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "transform",
    "data": "add timestamp to this"
  }' | jq '.'

# Test AAA0_0200_W - calculate
echo -e "\n5. Testing AAA0_0200_W - calculate..."
curl -s -X POST "$BASE_URL/api/cpp/aaa0_0200" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "calculate",
    "data": "some data to calculate"
  }' | jq '.'

# Test AAA0_0200_W - validate
echo -e "\n6. Testing AAA0_0200_W - validate..."
curl -s -X POST "$BASE_URL/api/cpp/aaa0_0200" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "validate",
    "data": "data to validate"
  }' | jq '.'

# Test generic call
echo -e "\n7. Testing Generic Worker Call..."
curl -s -X POST "$BASE_URL/api/cpp/call" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "AAA0_0100_W",
    "eventType": "echo",
    "data": "Generic call test"
  }' | jq '.'

echo -e "\n=================================="
echo "C++ Workers Tests Complete"
echo "=================================="
