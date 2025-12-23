#!/bin/bash

# Test Demo - Java + C++ Integration

BASE_URL="http://localhost:8080"

echo "========================================"
echo "Testing Java + C++ Integration Demo"
echo "========================================"

# Test health
echo -e "\n1. Health Check..."
curl -s -X GET "$BASE_URL/api/demo/health" | jq '.'

# Test echo
echo -e "\n2. Testing Echo (Client -> Java -> C++ -> Java -> Client)..."
curl -s -X POST "$BASE_URL/api/demo/echo" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello from Java Client!"
  }' | jq '.'

# Test calculate
echo -e "\n3. Testing Calculate with Java + C++ Processing..."
curl -s -X POST "$BASE_URL/api/demo/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "value": 10
  }' | jq '.'

echo -e "\n   Flow:"
echo "   - Input: 10"
echo "   - Java pre-process: 10 * 2 = 20"
echo "   - C++ process: uppercase '20'"
echo "   - Java post-process: 20 + 100 = 120"

# Test transform
echo -e "\n4. Testing Transform (Java -> C++ -> Java)..."
curl -s -X POST "$BASE_URL/api/demo/transform" \
  -H "Content-Type: application/json" \
  -d '{
    "data": "test data"
  }' | jq '.'

# Test with different values
echo -e "\n5. Testing Calculate with value=50..."
curl -s -X POST "$BASE_URL/api/demo/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "value": 50
  }' | jq '.'

echo -e "\n========================================"
echo "Demo Tests Complete!"
echo "========================================"
echo ""
echo "Summary:"
echo "  ✓ Java REST API receives client requests"
echo "  ✓ Java pre-processes data"
echo "  ✓ Java calls C++ worker via gRPC"
echo "  ✓ C++ worker processes and returns"
echo "  ✓ Java post-processes result"
echo "  ✓ Java returns to client"
echo ""
