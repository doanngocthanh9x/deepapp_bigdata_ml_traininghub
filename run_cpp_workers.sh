#!/bin/bash

# Run C++ Workers only (for testing)

cd /root/deepapp/deepapp_main

SERVER_ADDRESS="${1:-72.60.111.138:50051}"
CLIENT_ID="${2:-cpp-worker}"

echo "========================================"
echo "Running DeepApp C++ Workers"
echo "========================================"
echo "Server: $SERVER_ADDRESS"
echo "Client ID: $CLIENT_ID"
echo "========================================"
echo ""

./build/deepapp_worker_main "$SERVER_ADDRESS" "$CLIENT_ID"
